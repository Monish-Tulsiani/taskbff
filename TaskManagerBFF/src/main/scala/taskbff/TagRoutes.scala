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

object TagRoutes {

  implicit def tagEntityDecoder[F[_]: Async]: EntityDecoder[F, Tag] = jsonOf[F, Tag]
  implicit def tagEntityEncoder[F[_]: Async]: EntityEncoder[F, Tag] = jsonEncoderOf[F, Tag]
  implicit def tagDataEntityDecoder[F[_]: Async]: EntityDecoder[F, TagData] = jsonOf[F, TagData]
  implicit def tagDataEntityEncoder[F[_]: Async]: EntityEncoder[F, TagData] = jsonEncoderOf[F, TagData]

  def tagRoutes[F[_]: Async: LiftIO: Logger](tagRepo: TagRepository): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      case req @ POST -> Root / "tags" =>
        for {
          tagInput <- req.as[TagData]
          _ <- Logger[F].info(s"Received request to create tag: $tagInput")
          tagWithId = Tag(UUID.randomUUID().toString, tagInput.name)
          result <- LiftIO[F].liftIO(tagRepo.createTag(tagWithId)).attempt
          response <- result match {
            case Right(_) => Created(tagWithId)
            case Left(e) => InternalServerError(s"Failed to create tag: ${e.getMessage}")
          }
        } yield response


      case GET -> Root / "tags" =>
        Logger[F].info("Received request to get all tags") >>
          LiftIO[F].liftIO(tagRepo.getAllTags).flatMap { tags =>
            Logger[F].info(s"Successfully retrieved tasks: $tags") >>
              Ok(tags)
          }.handleErrorWith { err =>
            Logger[F].error(err)("Failed to retrieve tags") >>
              InternalServerError("Something went wrong")
          }

      case GET -> Root / "tags" / id =>
        Logger[F].info(s"Received request to get task with id $id") >>
          LiftIO[F].liftIO(tagRepo.getTagById(id)).flatMap {
            case Some(tag) => Ok(tag)
            case None => NotFound(s"No task found with id $id")
          }

      case req @ PUT -> Root / "tags" / id =>
        req.as[TagData].flatMap { updatedTagInput =>
          Logger[F].info(s"Received request to update tag with id $id with data: $updatedTagInput") >>
            LiftIO[F].liftIO {
              tagRepo.updateTag(id, updatedTagInput.name)
            }.flatMap {
              case 1 => Ok(updatedTagInput)
              case _ => NotFound(s"No tag found with id $id")
            }
        }

      case DELETE -> Root / "tags" / id =>
        Logger[F].info(s"Received request to delete tag with id $id") >>
          LiftIO[F].liftIO(tagRepo.deleteTag(id)).flatMap {
            case 1 => NoContent()
            case _ => NotFound(s"No tag found with id $id")
          }
    }
  }
}