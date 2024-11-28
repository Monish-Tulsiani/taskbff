package taskbff

import doobie.Read

case class User(user_id: String, username: String, password: String, role_id: String)

case class UserData(username: String, password: String)

case class Role(role_id: String, role_name: String)

case class Tag(tag_id: String, name: String)
object Tag {
  implicit val tagRead: Read[Tag] = Read[(String, String)].map {
    case (id, name) =>
      Tag(id, name)
  }
}

case class TagData(name: String)

case class Status(status_id: String, status: String)

case class Task(
                 id: String,
                 task: String,
                 status: String,
                 tags: List[String],
                 user_id: String
               )
object Task {
  implicit val taskRead: Read[Task] = Read[(String, String, String, String, String)].map {
    case (id, task, status, tags, userId) =>
      val cleanTags = tags.stripPrefix("{").stripSuffix("}").split(",").map(_.trim.stripPrefix("\"").stripSuffix("\"")).toList
      Task(id, task, status, cleanTags, userId)
  }
}

case class TaskData(
                      task: String,
                      status: String,
                      tags: List[String],
                      userId: String
                    )