package taskbff

import cats.effect.IO
import doobie._
import doobie.implicits._
import org.typelevel.log4cats.Logger


class UserRepository(transactor: Transactor[IO])(implicit L: Logger[IO]) {

  def createUser(user: User): IO[Int] =
    sql"INSERT INTO users (user_id, username, password, role_id) VALUES (${user.user_id}, ${user.username}, ${user.password}, ${user.role_id})".update.run.transact(transactor)

  def getUserById(id: String): IO[Option[User]] =
    sql"SELECT user_id, username, password, role_id FROM users WHERE user_id = $id".query[User].option.transact(transactor)

  def getAllUsers: IO[List[User]] =
    sql"SELECT user_id, username, password, role_id FROM users WHERE username != 'Admin'".query[User].to[List].transact(transactor)

  def updateUser(userData: UserData, id: String): IO[Int] =
    sql"UPDATE users SET username = ${userData.username}, password = ${userData.password} WHERE user_id = $id".update.run.transact(transactor)

  def deleteUser(id: String): IO[Int] =
    sql"DELETE FROM users WHERE user_id = $id".update.run.transact(transactor)

  def getUserByUsernameAndPassword(username: String, password: String): IO[Option[User]] =
    sql"SELECT user_id, username, password, role_id FROM users WHERE username = $username AND password = $password"
      .query[User]
      .option
      .transact(transactor)
}
