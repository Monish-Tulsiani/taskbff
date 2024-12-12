package taskbff

import cats.effect.{Async, LiftIO}
import cats.implicits._
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.{JWTVerificationException, TokenExpiredException}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{Challenge, EntityDecoder, EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.Logger

import java.util.Date

object AuthRoutes {

  private val SecretKey = "QWERTY-SECRET-KEY"
  private val jwtAlgorithm = Algorithm.HMAC256(SecretKey)

  private val AccessTokenExpirationTimeMs = 60000*5 // 1 minutes

  implicit def loginEntityDecoder[F[_] : Async]: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]

  implicit def loginEntityEncoder[F[_] : Async]: EntityEncoder[F, LoginResponse] = jsonEncoderOf[F, LoginResponse]

  implicit def refreshEntityDecoder[F[_] : Async]: EntityDecoder[F, TokenRefreshRequest] = jsonOf[F, TokenRefreshRequest]

  implicit def refreshEntityEncoder[F[_] : Async]: EntityEncoder[F, TokenRefreshResponse] = jsonEncoderOf[F, TokenRefreshResponse]

  def authRoutes[F[_] : Async : LiftIO : Logger](userRepo: UserRepository): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      // Login: Validate credentials and generate a JWT token
      case req@POST -> Root / "login" =>
        for {
          loginRequest <- req.as[LoginRequest]
          userOpt <- LiftIO[F].liftIO(userRepo.getUserByUsernameAndPassword(loginRequest.username, loginRequest.password))
          response <- userOpt match {
            case Some(user) =>
              val accessToken = generateJwtToken(user, AccessTokenExpirationTimeMs)
              Ok(LoginResponse(accessToken))
            case None =>
              val challenge = Challenge("Bearer", "Invalid username or password")
              Unauthorized(challenge)
          }
        } yield response

      // Refresh: Validate expired JWT and generate a new token
      case req @ POST -> Root / "refresh" =>
        for {
          refreshRequest <- req.as[TokenRefreshRequest]
          response <- validateJwtToken(refreshRequest.token) match {

            case ValidToken(_) =>
              Ok("Token is still valid. No need to refresh.")

            case ExpiredToken =>
              val decodedToken = JWT.decode(refreshRequest.token)
              val userId = decodedToken.getClaim("id").asString()

              LiftIO[F].liftIO(userRepo.getUserById(userId.toInt)).flatMap {
                case Some(user) =>
                  val newToken = generateJwtToken(user, AccessTokenExpirationTimeMs)
                  Ok(TokenRefreshResponse(newToken))
                case None =>
                  Forbidden("User not found for the given token")
              }

            case InvalidToken(error) =>
              Forbidden(s"Invalid token: $error")
          }
        } yield response
    }
  }

  private def generateJwtToken(user: User, expirationTime: Long): String = {
    JWT.create()
      .withClaim("id", user.user_id.toString)
      .withClaim("username", user.username)
      .withClaim("role", user.role_id.toString)
      .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
      .sign(jwtAlgorithm)
  }

  sealed trait TokenValidationResult
  case class ValidToken(userId: String) extends TokenValidationResult
  case object ExpiredToken extends TokenValidationResult
  case class InvalidToken(error: String) extends TokenValidationResult

  def validateJwtToken(token: String): TokenValidationResult = {
    try {
      val verifier = JWT.require(jwtAlgorithm).build()
      verifier.verify(token) // Throws if token is invalid or expired

      val decodedToken = JWT.decode(token)
      ValidToken(decodedToken.getClaim("id").asString())

    } catch {
      case _: TokenExpiredException =>
        ExpiredToken

      case e: JWTVerificationException =>
        InvalidToken(s"JWT verification failed: ${e.getMessage}")

      case e: Exception =>
        InvalidToken(s"Error decoding token: ${e.getMessage}")
    }
  }
}

case class LoginRequest(username: String, password: String)
case class LoginResponse(accessToken: String)
case class TokenRefreshRequest(token: String)
case class TokenRefreshResponse(accessToken: String)