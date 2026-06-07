package ru.otus.module1

import ru.otus.module1.utils.NameableThreads

import java.io.File
import java.util.concurrent.{ExecutorService, Executors}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

object concurrency {

  class MyThread extends Thread {
    override def run(): Unit =
      println(s"Hello from ${Thread.currentThread().getName}")
  }

  def printRunningTime(f: => Unit): Unit = {
    val start = System.currentTimeMillis()
    f
    val end = System.currentTimeMillis()
    println(s"Running time: ${end - start}")
  }

  def getRatesLocation1: ToyFuture[Int] = ToyFuture {
    Thread.sleep(1000)
    println("Location 1")
    10
  }(executors.pool1)

  def getRatesLocation2: ToyFuture[Int] = ToyFuture {
    Thread.sleep(2000)
    println("Location 2")
    20
  }(executors.pool1)

  def async(f: => Unit): Thread = new Thread {
    override def run(): Unit = f
  }

  def async2[A](f: => A): A = {
    var v: A = null.asInstanceOf[A]
    val t = new Thread {
      override def run(): Unit =
        v = f
    }
    t.start()
    v
  }

  trait ToyFuture[T] {
    def onComplete[U](f: Try[T] => U): Unit
    def flatMap[B](f: T => ToyFuture[B]): ToyFuture[B]
    def map[B](f: T => B): ToyFuture[B]
    def isCompleted: Boolean
  }

  trait ToyPromise[T] {
    def complete(result: Try[T]): Unit
    def isCompleted: Boolean
    def future: ToyFuture[T]
  }

  class ToyFutureImpl[T]private(v: Option[() => T], ex: ExecutorService) extends ToyFuture [T] with ToyPromise [T]{
    private var r: Try[T] = null.asInstanceOf[Try[T]]
    private var _isCompleted: Boolean = false
    private val q = mutable.Queue[Try[T] => _]()

    override def isCompleted: Boolean = _isCompleted

    override def future: ToyFuture[T] = this

    def onComplete[U](f: Try[T] => U): Unit =
      if (_isCompleted) f(r)
      else q.enqueue(f)

    def flatMap[B](f: T => ToyFuture[B]): ToyFuture[B] = {
      val result = ToyPromise[B]()
      this onComplete {
        case Success(value) =>
          f(value).onComplete{ b =>
            result.complete(b)
          }
        case Failure(exception) =>
          result.complete(Try(throw new Exception(exception.getMessage)))
      }
      result.future
    }

    def map[B](f: T => B): ToyFuture[B] = flatMap(v => ToyFuture[B](f(v))(ex))


    def complete(result: Try[T]): Unit =
      if(!isCompleted){
        r = result
        _isCompleted = true
        while (q.nonEmpty) {
          q.dequeue()(r)
        }
      }


    private def start() = {
      val t = new Runnable {
        override def run(): Unit = {
          r = Try(v.get())
          _isCompleted = true
          while (q.nonEmpty) {
            q.dequeue()(r)
          }
        }
      }
      ex.execute(t)
    }
  }

  private object ToyFutureImpl {
    def apply[T](v: => T)(ex: ExecutorService): ToyFuture[T] = {
      val f = new ToyFutureImpl[T](Some(() => v), ex)
      f.start()
      f
    }

    def pending[T](ex: ExecutorService): ToyFutureImpl[T] =
      new ToyFutureImpl[T](None, ex)
  }

  object ToyFuture {
    def apply[T](v: => T)(ex: ExecutorService): ToyFuture[T] =
      ToyFutureImpl.apply(v)(ex)
  }

  object ToyPromise {
    def apply[T](): ToyPromise[T] = ToyFutureImpl.pending[T](null)
  }


  object try_ {

    def readFromFile(): List[String] = {
      val s: BufferedSource = Source.fromFile(new File("ints.txt"))
      val result: List[String] = try {
        s.getLines().toList
      } catch {
        case e =>
          println(e.getMessage)
          Nil
      } finally {
        s.close()
      }

      result
    }

    def readFromFile2(): Try[List[String]] = {
      val sTry: Try[BufferedSource] = Try(Source.fromFile(new File("ints.txt")))
      val result = for{
        bs <- sTry
        r <- Try(bs.getLines().toList)
      } yield r
      sTry.foreach(_.close())
      result
    }

    readFromFile().foreach(println)
    readFromFile2() match {
      case Failure(exception) =>
        println(exception.getMessage)
      case Success(value) => value.foreach(println)
    }
  }

  object future {
    // constructors


    val f1: Future[Int] = Future.successful(10)
    val f2: Future[Int] = Future.failed[Int](new Throwable("Ooops"))
    val f3: Future[Int] = Future.fromTry(Try(10))
    val f4: Future[Int] = Future(10)(ec)


    // Execution context
    lazy val ec = ExecutionContext.fromExecutor(executors.pool1)
    lazy val ec1 = ExecutionContext.fromExecutor(executors.pool2)
    lazy val ec3 = ExecutionContext.fromExecutor(executors.pool3)
    lazy val ec4 = ExecutionContext.fromExecutor(executors.pool4)


    def printRunningTime[T](v: => Future[T]): Future[T] = {
      val start = Future.successful(System.currentTimeMillis())
//      start.flatMap{ s =>
//        v.flatMap{ t =>
//          .flatMap{e =>
//            println(s"Running time: ${e - s}")
//            Future.successful(t)
//          }(ec)
//        }(ec)
//      }(ec)
      given ExecutionContext = ec1

      for{
        s <- start
        r <- v
        e <- Future.successful(System.currentTimeMillis())
        _ <- Future.successful(println(s"Running time: ${e - s}"))
      } yield r
    }

    def getRatesLocation1: Future[Int] = Future {
      Thread.sleep(1000)
      println("Location 1")
      10
    }(ec)

    def getRatesLocation2: Future[Int] = Future {
      Thread.sleep(2000)
      println("Location 2")
      20
    }(ec)

    def rates: Future[(Int, Int)] = {
      val r1 = getRatesLocation1
      val r2 = getRatesLocation2
      r1.zip(r2)
    }



    // combinators
    def longRunningComputation: Int = ???


    def action(v: Int): Int = {
      Thread.sleep(1000)
      println(s"Action $v in ${Thread.currentThread().getName}")
      v
    }

    val f01 = Future(action(10))(ec)
    val f02 = Future(action(20))(ec1)

    val f03 = f01.flatMap{ v1 =>
      action(50)
      f02.map{ v2 =>
        action(v1 + v2)
      }(ec4)
    }(ec3)



    // Execution contexts


  }

  object promise {

    val p = Promise[Int]
    println(p.isCompleted) // false
    val f1: Future[Int] = p.future
    println(f1.isCompleted) // false
    p.complete(Try(10))
    println(p.isCompleted) // true
    println(f1.isCompleted) // true
  }









}

object executors {
  val pool1: ExecutorService =
    Executors.newFixedThreadPool(2, NameableThreads("fixed-pool-1"))
  val pool2: ExecutorService =
    Executors.newCachedThreadPool(NameableThreads("cached-pool-2"))
  val pool3: ExecutorService =
    Executors.newWorkStealingPool(4)
  val pool4: ExecutorService =
    Executors.newSingleThreadExecutor(NameableThreads("singleThread-pool-4"))
}


