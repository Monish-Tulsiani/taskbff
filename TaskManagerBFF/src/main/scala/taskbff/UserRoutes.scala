package taskbff

import cats.effect.{Async, LiftIO}
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.Logger

import java.util.UUID

object UserRoutes {

  implicit def userEntityDecoder[F[_]: Async]: EntityDecoder[F, User] = jsonOf[F, User]
  implicit def userEntityEncoder[F[_]: Async]: EntityEncoder[F, User] = jsonEncoderOf[F, User]
  implicit def userInputEntityDecoder[F[_]: Async]: EntityDecoder[F, UserData] = jsonOf[F, UserData]
  implicit def userInputEntityEncoder[F[_]: Async]: EntityEncoder[F, UserData] = jsonEncoderOf[F, UserData]
  implicit def listUserEntityEncoder[F[_]: Async]: EntityEncoder[F, List[User]] = jsonEncoderOf[F, List[User]]


  def userRoutes[F[_]: Async: LiftIO: Logger](userRepo: UserRepository): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      case req @ POST -> Root / "users" =>
        for {
          userData <- req.as[UserData] // Extract the user data from the request body
          _ <- Logger[F].info(s"Received request to create user: $userData")
          val userWithRole = User(0, userData.username, userData.password, 2) // Set user_id to 0 for auto-increment
          result <- LiftIO[F].liftIO(userRepo.createUser(userWithRole)).attempt
          response <- result match {
            case Right(createdUser) => Created(createdUser) // Return the created user with ID
            case Left(e) => InternalServerError(s"Failed to create user: ${e.getMessage}") // Handle errors
          }
        } yield response


      case GET -> Root / "users" =>
        Logger[F].info("Received request to get all users") >>
          LiftIO[F].liftIO(userRepo.getAllUsers).flatMap { users =>
            Logger[F].info(s"Successfully retrieved tasks: $users") >>
              Ok(users)
          }.handleErrorWith { err =>
            Logger[F].error(err)("Failed to retrieve users") >>
              InternalServerError("Something went wrong")
          }

      case GET -> Root / "users" / IntVar(id) =>
        Logger[F].info(s"Received request to get user with id $id") >>
          LiftIO[F].liftIO(userRepo.getUserById(id)).flatMap {
            case Some(user) => Ok(user)
            case None => NotFound(s"No user found with id $id")
          }

      case req @ PUT -> Root / "users" / IntVar(id) =>
        req.as[UserData].flatMap { userData =>
          Logger[F].info(s"Received request to update user with id $id with data: $userData") >>
            LiftIO[F].liftIO {
              userRepo.updateUser(userData, id)
            }.flatMap {
              case 1 => Ok(userData)
              case _ => NotFound(s"No user found with id $id")
            }
        }

      case DELETE -> Root / "users" / IntVar(id) =>
        Logger[F].info(s"Received request to delete user with id $id") >>
          LiftIO[F].liftIO(userRepo.deleteUser(id)).flatMap {
            case 1 => NoContent()
            case _ => NotFound(s"No user found with id $id")
          }
    }
  }
}