package ru.otus.module3

import zio.{Promise, Ref, Schedule, UIO, ZIO, durationInt}

import java.util.concurrent.atomic.AtomicReference
import scala.language.postfixOps

object zioDS {

  object schedule {

    val eff = ZIO.attempt(println("Hello"))

    /** 1. Написать эффект, который будет выводить в консоль Hello 5 раз
     */

    val schedule1 = Schedule.recurs(5)

    val eff1 = eff.repeat(schedule1)


    /** 2. Написать эффект, который будет выводить в консоль Hello 5 раз, с интервалом в 1 секунду
     */

    val schedule2 = Schedule.spaced(1 second)
    val eff2 = eff.repeat(schedule1 && schedule2)



    /** Написать эффект, который будет генерить произвольное число от 0 до 10,
     * и повторяться пока число не будет равным 0
     */

    val random = zio.Random.nextIntBetween(0, 11)
    val schedule3 = Schedule.recurUntil[Int](_ == 0)
    val eff3 = random.repeat(schedule3)



    /** Написать планировщик, который будет выполняться каждую пятницу 12 часов дня
     */

    val schedule4 = Schedule.dayOfWeek(5) && Schedule.hourOfDay(12)


  }

  object ref {

    /**
     * Счетчик
     *
     */

    var counter: Int = 0
    
    val updateCounter: UIO[Int] = ZIO.foreachPar(1 to 3){ _ =>
      ZIO.succeed(counter += 1)
    }.as(counter)

    val updateCounterRef: UIO[Int] = for{
      counter <- Ref.make(0)
      _ <- ZIO.foreachPar(1 to 3){ _ =>
        counter.update(_ + 1)
      }
      result <- counter.get
    } yield result
    

    trait ToyRef[A] {
      def modify[B](f: A => (B, A)): UIO[B]

      def get: UIO[A] = modify(a => (a, a))

      def set(a: A): UIO[Unit] = modify(_ => ((), a))

      def update[B](f: A => A): UIO[Unit] =
        modify(a => ((), f(a)))
    }
    
    object ToyRef {
      def make[A](a: A): UIO[ToyRef[A]] = ZIO.succeed{
        new ToyRef[A] {
          val atomic = new AtomicReference(a)
          override def modify[B](f: A => (B, A)): UIO[B] = ZIO.succeed{
            var cond = true
            var r: B = null.asInstanceOf[B]
            while (cond){
              val current = atomic.get()
              val tuple = f(current)
              r = tuple._1
              cond = !atomic.compareAndSet(current, tuple._2)
            }
            r
          }
        }
      }
    }



  }
}
