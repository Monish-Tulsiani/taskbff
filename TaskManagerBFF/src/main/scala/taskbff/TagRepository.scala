package taskbff

import cats.effect.IO
import doobie._
import doobie.implicits._
import org.typelevel.log4cats.Logger


class TagRepository(transactor: Transactor[IO])(implicit L: Logger[IO]) {

  def createTag(tag: Tag): IO[Int] =
    sql"INSERT INTO tags (tag_id, name) VALUES (${tag.tag_id}, ${tag.name})".update.run.transact(transactor)

  def getTagById(id: String): IO[Option[Tag]] =
    sql"SELECT tag_id, name FROM tags WHERE tag_id = $id".query[Tag].option.transact(transactor)

  def getAllTags: IO[List[Tag]] =
    sql"SELECT tag_id, name FROM tags".query[Tag].to[List].transact(transactor)

  def updateTag(id: String, name: String): IO[Int] =
    sql"UPDATE tags SET name = $name WHERE tag_id = $id".update.run.transact(transactor)

  def deleteTag(id: String): IO[Int] =
    sql"DELETE FROM tags WHERE tag_id = $id".update.run.transact(transactor)
}
