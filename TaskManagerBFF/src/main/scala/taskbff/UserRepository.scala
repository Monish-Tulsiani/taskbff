package taskbff

import cats.effect.IO
import doobie._
import doobie.implicits._
import org.typelevel.log4cats.Logger


class UserRepository(transactor: Transactor[IO])(implicit L: Logger[IO]) {

  def createUser(user: User): IO[User] = {
    sql"INSERT INTO users (username, password, role_id) VALUES (${user.username}, ${user.password}, ${user.role_id})"
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .map(userId => user.copy(user_id = userId)) // Return the user with the generated user_id
      .transact(transactor)
  }
  def getUserById(id: Int): IO[Option[User]] =
    sql"SELECT id, username, password, role_id FROM users WHERE id = $id".query[User].option.transact(transactor)

  def getAllUsers: IO[List[User]] =
    sql"SELECT id, username, password, role_id FROM users WHERE username != 'Admin'".query[User].to[List].transact(transactor)

  def updateUser(userData: UserData, id: Int): IO[Int] =
    sql"UPDATE users SET username = ${userData.username}, password = ${userData.password} WHERE id = $id".update.run.transact(transactor)

  def deleteUser(id: Int): IO[Int] =
    sql"DELETE FROM users WHERE id = $id".update.run.transact(transactor)

  def getUserByUsernameAndPassword(username: String, password: String): IO[Option[User]] =
    sql"SELECT id, username, password, role_id FROM users WHERE username = $username AND password = $password"
      .query[User]
      .option
      .transact(transactor)
}
