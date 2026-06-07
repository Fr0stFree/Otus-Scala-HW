package ru.otus.module1.futures

object HomeworksUtils {

  private final case class TaskNotDone(text: String)
    extends RuntimeException(s"выполните задание: \n $text")

  trait TaskDef {
    def applySeq(num: Seq[Int]): Nothing

    def apply(num: Int*): Nothing = applySeq(num)
  }

  extension (cs: StringContext)
    def task(refs: Any*): TaskDef = _ => {
      val message = cs.s(refs: _*).stripMargin
      throw TaskNotDone(message)
    }

}
