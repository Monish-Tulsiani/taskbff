package taskbff

import cats.effect.IO
import doobie._
import doobie.implicits._
import org.typelevel.log4cats.Logger

import java.util.UUID

class TaskRepository(transactor: Transactor[IO])(implicit L: Logger[IO]) {

  def createTask(task: Task): IO[Int] = {
    val taskId: String = UUID.randomUUID().toString
    val tagsArray = task.tags.mkString(",")
    val insertQuery = fr"""
      INSERT INTO tasks (id, task, status, tags)
      VALUES ($taskId, ${task.task}, ${task.status}, ARRAY[$tagsArray]::text[], ${task.user_id})
    """
    insertQuery.update.run.transact(transactor)
  }

  def getTaskById(id: String): IO[Option[Task]] = {
    L.info(s"Retrieving task with id $id") >>
      sql"SELECT task_id, task, status, tags, user_id FROM tasks WHERE task_id = $id"
        .query[Task]
        .option
        .transact(transactor)
  }

  def getAllTasks: IO[List[Task]] =
    L.info("Retrieving all tasks") >>
      sql"SELECT task_id, task, status, tags, user_id FROM tasks;"
        .query[Task]
        .to[List]
        .transact(transactor)
        .flatTap(tasks => L.info(s"Retrieved tasks: $tasks"))

  def updateTask(id: String, task: Task): IO[Int] =
    L.info(s"Updating task with id $id with data: $task") >>
      sql"UPDATE tasks SET task = ${task.task}, status = ${task.status}, tags = ARRAY[${task.tags.mkString(",")}]::text[] WHERE task_id = $id"
        .update.run
        .transact(transactor)
        .flatTap(updated =>
          if (updated == 1) L.info(s"Task updated successfully with id $id") else L.warn(s"No task found with id $id")
        )

  def deleteTask(id: String): IO[Int] =
    L.info(s"Deleting task with id $id") >>
      sql"DELETE FROM tasks WHERE task_id = $id"
        .update.run
        .transact(transactor)
        .flatTap(deleted =>
          if (deleted == 1) L.info(s"Task deleted successfully with id $id") else L.warn(s"No task found with id $id")
        )
}