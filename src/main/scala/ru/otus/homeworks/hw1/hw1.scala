package ru.otus.homeworks.hw1

object hw1 {

  /** Реализуем тип Option
    */

  object opt {

    /** Реализовать структуру данных Option, который будет указывать на
      * присутствие либо отсутствие результата
      */

    sealed trait Option[+T] {
      def isEmpty: Boolean = if (this.isInstanceOf[None.type]) true else false

      def map[B](f: T => B): Option[B] = flatMap(v => Option(f(v)))

      def flatMap[B](f: T => Option[B]): Option[B] =
        if (isEmpty) None
        else f(this.asInstanceOf[Some[T]].v)
    }

    object Option {
      def apply[T](v: T): Option[T] = Some(v)
    }

    case class Some[T](v: T) extends Option[T]
    case object None extends Option[Nothing]

    /** Реализовать метод printIfAny, который будет печатать значение, если оно
      * есть
      */

    def printIfAny[T](opt: Option[T]): Unit = opt.map(println)

    /** Реализовать метод zip, который будет создавать Option от пары значений
      * из 2-х Option
      */

    def zip[A, B](optA: Option[A], optB: Option[B]): Option[(A, B)] =
      optA.flatMap(a => optB.map(b => (a, b)))

    /** Реализовать метод filter, который будет возвращать не пустой Option в
      * случае если исходный не пуст и предикат от значения = true
      */

    def filter[A](opt: Option[A], predicate: A => Boolean): Option[A] =
      opt.flatMap(elem => if (predicate(elem)) then Some(elem) else None)
  }

  object list {

    /** Реализовать одно связанный иммутабельный список List Список имеет два
      * случая: Nil - пустой список Cons - непустой, содержит первый элемент
      * (голову) и хвост (оставшийся список)
      */

    sealed trait List[+T] {
      def ::[TT >: T](elem: TT): List[TT] =
        new ::(elem, this)
    }

    case class ::[A](head: A, tail: List[A]) extends List[A]
    case object Nil extends List[Nothing]

    object List {
      def apply[A](v: A*): List[A] =
        if (v.isEmpty) Nil else ::(v.head, apply(v.tail: _*))
    }

    /** Конструктор, позволяющий создать список из N - го числа аргументов Для
      * этого можно воспользоваться *
      *
      * Например, вот этот метод принимает некую последовательность аргументов с
      * типом Int и выводит их на печать def printArgs(args: Int*) =
      * args.foreach(println(_))
      */

    /** Реализовать метод reverse который позволит заменить порядок элементов в
      * списке на противоположный
      */

    def reverse[A](list: List[A], accumulator: List[A] = Nil): List[A] =
      list match
        case Nil          => accumulator
        case head :: tail => reverse(tail, head :: accumulator)

    /** Реализовать метод map для списка который будет применять некую ф-цию к
      * элементам данного списка
      */

    def map[A, B](func: A => B, list: List[A]): List[B] = list match
      case Nil          => Nil
      case head :: tail => func(head) :: map(func, tail)

    /** Реализовать метод filter для списка который будет фильтровать список по
      * некому условию
      */

    def filter[A](predicate: A => Boolean, list: List[A]): List[A] = list match
      case Nil                             => Nil
      case head :: tail if predicate(head) => head :: filter(predicate, tail)
      case _ :: tail                       => filter(predicate, tail)

    /** Написать функцию incList которая будет принимать список Int и возвращать
      * список, где каждый элемент будет увеличен на 1
      */

    def incList(list: List[Int]): List[Int] =
      map((number: Int) => number + 1, list)

    /** Написать функцию shoutString которая будет принимать список String и
      * возвращать список, где к каждому элементу будет добавлен префикс в виде
      * '!'
      */

    def shoutString(list: List[String]): List[String] =
      map((word: String) => "!".concat(word), list)
  }
}
