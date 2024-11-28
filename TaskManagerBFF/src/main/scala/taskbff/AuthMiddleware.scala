package taskbff

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.{Challenge, HttpRoutes, Request}
import org.typelevel.log4cats.Logger
import cats.syntax.apply._
import org.http4s._


object AuthMiddleware {

  def apply[F[_]: Sync: Logger](validateToken: String => Either[String, String]): HttpRoutes[F] => HttpRoutes[F] = { httpRoutes =>
    Kleisli { req: Request[F] =>
      val dsl = Http4sDsl[F]
      import dsl._

      extractToken(req) match {
        case Some(token) =>
          validateToken(token) match {
            case Right(userId) =>
              httpRoutes.run(req) // Forward the request as the token is valid
            case Left(error) =>
              // Log the invalid token error and return Forbidden wrapped in OptionT
              OptionT.liftF(Logger[F].warn(s"Invalid token: $error") *> Forbidden("Invalid token"))
          }
        case None =>
          // Log the missing token error and return Unauthorized wrapped in OptionT
          OptionT.liftF(Logger[F].warn("Missing Authorization header") *> Unauthorized(Challenge("Bearer", "Protected resource", Map("error" -> "missing_token"))))
      }
    }
  }

  private def extractToken[F[_]: Sync](req: Request[F]): Option[String] = {
    req.headers
      .get[Authorization]
      .flatMap {
        case Authorization(Credentials.Token(_, token)) => Some(token)
        case _ => None
      }
  }
}