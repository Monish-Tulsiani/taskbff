package taskbff

import cats.effect._
import cats.syntax.semigroupk._
import doobie.hikari.HikariTransactor
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.middleware.CORSConfig
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object ServerApp extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def transactor: Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5432/taskdb",
      "postgresuser",
      "postgrespassword",
      ExecutionContext.global
    )
  }

  override def run(args: List[String]): IO[ExitCode] = {

    transactor.use { xa =>
      val taskRepo = new TaskRepository(xa)
      val tagRepo = new TagRepository(xa)
      val userRepo = new UserRepository(xa)

      val taskRoutes = TaskRoutes.taskRoutes[IO](taskRepo)
      val tagRoutes = TagRoutes.tagRoutes[IO](tagRepo)
      val userRoutes = UserRoutes.userRoutes[IO](userRepo)
      val authRoutes = AuthRoutes.authRoutes[IO](userRepo)

      val securedTaskRoutes = AuthMiddleware[IO](AuthRoutes.validateJwtToken)(Sync[IO], logger)(taskRoutes)
      val securedTagRoutes = AuthMiddleware[IO](AuthRoutes.validateJwtToken)(Sync[IO], logger)(tagRoutes)

      val corsConfig = CORSConfig(
        anyOrigin = true,  // Allow all origins
        allowedMethods = Some(Set("GET", "POST", "PUT", "DELETE", "OPTIONS")),
        allowedHeaders = Some(Set("Content-Type", "Authorization")),
        allowCredentials = true,
        maxAge = 3600
      )

      val httpApp = CORS(
        (authRoutes <+> userRoutes <+> securedTagRoutes <+> securedTaskRoutes).orNotFound,
        corsConfig
      )

      BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(9080, "localhost")
        .withHttpApp(httpApp)
        .resource
        .use { _ =>
          logger.info("Server started on http://localhost:9080") >> IO.never
        }
        .as(ExitCode.Success)
    }
  }
}