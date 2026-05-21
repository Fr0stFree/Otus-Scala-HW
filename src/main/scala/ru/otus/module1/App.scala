package ru.otus.module1

import ru.otus.module1.collections.ListLike


object App {
  def main(args: Array[String]): Unit = {
    println("Hello world")
    
    ListLike(1, 2, 3, 4, 5).foreach(println)
  }
}
