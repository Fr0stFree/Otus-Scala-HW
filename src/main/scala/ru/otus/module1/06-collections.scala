package ru.otus.module1

import scala.annotation.tailrec


object collections2 {


  sealed trait ToyList[+T] {
    def head: T
    def tail: ToyList[T]

    def isEmpty: Boolean = this match {
      case Nil => true
      case Cons(_, _) => false
    }

    def ::[TT >: T](v: TT): ToyList[TT] = Cons(v, this)

    def reverse: ToyList[T] = {
      def go(l: ToyList[T], acc: ToyList[T]): ToyList[T] = l match {
        case Cons(head, tail) => go(tail, head :: acc)
        case Nil => acc
      }
      go(this, Nil)
    }

    def concat[TT >: T](that: ToyList[TT]): ToyList[TT] = {
      def go(a: ToyList[TT], b: ToyList[TT], acc : ToyList[TT]): ToyList[TT] = {
        if(a.isEmpty && b.isEmpty) acc
        else if(!a.isEmpty) go(a.tail, b, a.head :: acc)
        else go(a, b.tail, b.head :: acc)
      }
      go(this, that, Nil).reverse
    }

    def flatMap[B](f: T => ToyList[B]): ToyList[B] = this match {
      case Cons(head, tail) => f(head).concat(tail.flatMap(f))
      case Nil => Nil
    }

    def map[B](f: T => B): ToyList[B] = flatMap(v => ToyList(f(v)))

    def foldLeft[B](acc: B)(op : (B, T) => B): B = this match {
      case Cons(head, tail) => tail.foldLeft(op(acc, head))(op)
      case Nil => acc
    }

    def take(n: Int): ToyList[Int] = ???

    def drop(n: Int): ToyList[Int] = ???

  }

  case class Cons[T](head: T, tail: ToyList[T]) extends ToyList[T]

  case object Nil extends ToyList[Nothing]{
    def head = throw new NoSuchElementException("Nil.head")
    def tail = throw new UnsupportedOperationException("Nil.tail")
  }

  object ToyList {
    def empty[T]: ToyList[T] = Nil
    def apply[T](el: T*): ToyList[T] = if(el.isEmpty) Nil else Cons(el.head, apply(el.tail:_*))
  }

}