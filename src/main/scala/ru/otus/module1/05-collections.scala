package ru.otus.module1

object collections {

    sealed trait ListLike[+A] extends Iterable[A] {
        self =>
        override def iterator: Iterator[A] = new Iterator[A] {
            private var coll = self

            override def hasNext: Boolean = coll match {
                case Cons(_, _) => true
                case Nil => false
            }

            override def next(): A = coll match {
                case Cons(head, tail) =>
                    coll = tail
                    head
                case Nil => throw new Exception("Next on empty iterator")
            }
        }
    }

    case class Cons[A](override val head: A, override val tail: ListLike[A]) extends ListLike[A]

    case object Nil extends ListLike[Nothing]

    object ListLike {
        def apply[A](v: A*): ListLike[A] =
            if (v.isEmpty) Nil else Cons(v.head, apply(v.tail: _*))
    }

    // удвоить числа в списке
    val numbers = List(1, 2, 3, 4, 5)
    val _ = numbers.map(_ * 2)

    // список всех чисел из списков
    val nestedLists = List(List(1, 2, 3), List(4, 5, 6))
    val _: List[Int] = nestedLists.flatten

    val words = List("Hello", "World")

    // преобразовать список слов, в список букв
    // map(f: A => B): List[B] f: String => List[Char]
    val chars = words.flatMap(_.toList)

    // посчитать кол-во символов во всех словах

    val totalChars = chars.size

    // найти все слова больше 3-ч символов

    val shortWords = words.filter(_.length > 3).flatMap(_.toList).sorted

    // найти сумму чисел в списке

    val sum = numbers.fold(1)((acc, v) => acc * v)

    // отсортировать список наших чисел
    val sorted = numbers.sortWith(_ > _)
}