package taskbff

import cats.effect.IO
import doobie._
import doobie.implicits._
import org.typelevel.log4cats.Logger


class TagRepository(transactor: Transactor[IO])(implicit L: Logger[IO]) {

  def createTag(tag: Tag): IO[Int] =
    sql"INSERT INTO tags (name) VALUES (${tag.name})".update.withUniqueGeneratedKeys[Int]("id").transact(transactor)

  def getTagById(id: Int): IO[Option[Tag]] =
    sql"SELECT id, name FROM tags WHERE id = $id".query[Tag].option.transact(transactor)

  def getAllTags: IO[List[Tag]] =
    sql"SELECT id, name FROM tags".query[Tag].to[List].transact(transactor)

  def updateTag(id: Int, name: String): IO[Int] =
    sql"UPDATE tags SET name = $name WHERE id = $id".update.run.transact(transactor)

  def deleteTag(id: Int): IO[Int] =
    sql"DELETE FROM tags WHERE id = $id".update.run.transact(transactor)
}
