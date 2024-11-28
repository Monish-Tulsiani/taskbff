package taskbff

import cats.effect.{Async, LiftIO}
import cats.implicits._
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{Challenge, EntityDecoder, EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.Logger

import java.util.Date

object AuthRoutes {

  private val SecretKey = "your-very-secret-key"
  private val jwtAlgorithm = Algorithm.HMAC256(SecretKey)

  private val AccessTokenExpirationTimeMs = 120000
  private val RefreshTokenExpirationTimeMs = 7 * 24 * 60 * 60 * 1000 // 7 days

  implicit def loginEntityDecoder[F[_]: Async]: EntityDecoder[F, LoginRequest] = jsonOf[F, LoginRequest]
  implicit def loginEntityEncoder[F[_]: Async]: EntityEncoder[F, LoginResponse] = jsonEncoderOf[F, LoginResponse]
  implicit def refreshEntityDecoder[F[_]: Async]: EntityDecoder[F, RefreshRequest] = jsonOf[F, RefreshRequest]
  implicit def refreshEntityEncoder[F[_]: Async]: EntityEncoder[F, RefreshResponse] = jsonEncoderOf[F, RefreshResponse]

  def authRoutes[F[_]: Async: LiftIO: Logger](userRepo: UserRepository): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      // Login: Validate credentials and generate both tokens
      case req @ POST -> Root / "login" =>
        for {
          loginRequest <- req.as[LoginRequest]
          userOpt <- LiftIO[F].liftIO(userRepo.getUserByUsernameAndPassword(loginRequest.username, loginRequest.password))
          response <- userOpt match {
            case Some(user) =>
              val accessToken = generateJwtToken(user, AccessTokenExpirationTimeMs)
              val refreshToken = generateJwtToken(user, RefreshTokenExpirationTimeMs)
              Ok(LoginResponse(accessToken, refreshToken))
            case None =>
              val challenge = Challenge("Bearer", "Invalid username or password")
              Unauthorized(challenge)
          }
        } yield response

      // Refresh: Validate refresh token and generate a new access token
      case req @ POST -> Root / "refresh" =>
        for {
          refreshRequest <- req.as[RefreshRequest]
          response <- validateJwtToken(refreshRequest.refreshToken) match {
            case Right(userId) =>
              LiftIO[F].liftIO(userRepo.getUserById(userId)).flatMap {
                case Some(user) =>
                  val accessToken = generateJwtToken(user, AccessTokenExpirationTimeMs)
                  Ok(RefreshResponse(accessToken))
                case None =>
                  Forbidden("User not found for the given refresh token")
              }
            case Left(error) =>
              Forbidden(s"Invalid refresh token: $error")
          }
        } yield response
    }
  }

  private def generateJwtToken(user: User, expirationTime: Long): String = {
    JWT.create()
      .withClaim("id", user.user_id)
      .withClaim("username", user.username)
      .withClaim("role", user.role_id)
      .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
      .sign(jwtAlgorithm)
  }

  def validateJwtToken(token: String): Either[String, String] = {
    try {
      val verifier = JWT.require(jwtAlgorithm).build()
      val decodedToken = verifier.verify(token)
      Right(decodedToken.getClaim("id").asString()) // Returning user ID
    } catch {
      case e: JWTVerificationException => Left(e.getMessage)
    }
  }
}

case class LoginRequest(username: String, password: String)
case class LoginResponse(accessToken: String, refreshToken: String)
case class RefreshRequest(refreshToken: String)
case class RefreshResponse(accessToken: String)