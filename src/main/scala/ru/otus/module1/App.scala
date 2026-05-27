package ru.otus.module1

import ru.otus.module1.collections.ListLike
import ru.otus.module1.collections2.ToyList


object App {
  def main(args: Array[String]): Unit = {
    println("Hello world")
    
    val l1 = ToyList(1, 2, 3)
    val l2 = ToyList(4, 5, 6)

    val ll1 = List(1, 2, 3)
    val ll2 = List(4, 5, 6)

    val l3 = for{
      e1 <- l1
      e2 <- l2
    } yield e1 + e2

    val ll3 = for {
      e1 <- ll1
      e2 <- ll2
    } yield e1 + e2

    val _l3 = l1.flatMap(_ => l2)

   val ll = LazyList(1, 2, 3)

    println(
      ll.map{v =>
      println(s"map: ${v}")
      v + 1
    }.filter{v =>
      println(s"Filter: ${v}")
      v % 2 == 0
    }.toList
    )

    println("For List")

    println(
      ll1.map { v =>
        println(s"map: ${v}")
        v + 1
      }.filter { v =>
        println(s"Filter: ${v}")
        v % 2 == 0
      }
    )

  }
}
