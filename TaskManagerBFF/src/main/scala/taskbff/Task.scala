package taskbff

import doobie.Read

case class User(user_id: Int, username: String, password: String, role_id: Int)

case class UserData(username: String, password: String)

case class Role(role_id: Int, role_name: String)

case class Tag(tag_id: Int, name: String)
object Tag {
  implicit val tagRead: Read[Tag] = Read[(Int, String)].map {
    case (id, name) =>
      Tag(id, name)
  }
}

case class TagData(name: String)

case class Status(status_id: String, status: String)

case class Task(
                 id: Int,
                 task: String,
                 status: Int,
                 tags: List[String],
                 user_id: Int
               )
object Task {
  implicit val taskRead: Read[Task] = Read[(Int, String, Int, String, Int)].map {
    case (id, task, statusId, tags, userId) =>
      val cleanTags = tags.stripPrefix("{").stripSuffix("}").split(",").map(_.trim.stripPrefix("\"").stripSuffix("\"")).toList
      Task(id, task, statusId, cleanTags, userId)
  }
}

case class TaskData(
                      task: String,
                      statusId: Int,
                      tags: List[String],
                      userId: Int
                    )