package ru.otus.module3

import ru.otus.module3.utils.Resource
import ru.otus.module3.tryFinally.zioResource
import ru.otus.module3.tryFinally.zioResource.{closeDummyFile, handleDummyFile, handleFile, openDummyFile}
import zio.{Scope, Task, Unsafe, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.IOException
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.{BufferedSource, Source}
import scala.language.{existentials, postfixOps}
import scala.util.{Failure, Success}
import tryFinally._


@main
def run9() = {
  Unsafe.unsafe { implicit unsafe =>
    zio.Runtime.default.unsafe.run(ZIO.scoped(zioScope.nFilesResult))
  }
}

object ResourceApp extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    zioScope.nFilesResult
}

object tryFinally {

  object traditional {


    def acquireResource: Resource = Resource("Some resource")

    def use(r: Resource): Unit = println(s"Using resource: ${r.name}")

    def releaseResource(r: Resource): Unit  = r.close()

    /**
     * Напишите код, который обеспечит корректную работу с ресурсом:
     * получить ресурс -> использовать -> освободить
     *
     */

    lazy val result = {
      val r = acquireResource
      try{
        use(r)
      } finally {
        releaseResource(r)
      }
    }

    /**
     *
     * Обобщенная версия работы с ресурсом
     */

    def withResource[R, A](resource: => R)(release: R => Any)(use: R => A): A = {
      val r = resource
      try{
        use(r)
      } finally {
        release(r)
      }
    }
    
    
    /**
     * Прочитать строки из файла
     */
    
    lazy val r = withResource(acquireResource)(releaseResource)(use)

  }

  object future{

    def acquireFutureResource = Future(Resource("Future resource"))

    def use(resource: Resource) = Future(println(s"Using ${resource.name}"))

    def releaseFutureResource(resource: Resource) = Future(resource.close())

    /**
     * Написать вспомогательный оператор ensuring, который позволит корректно работать
     * с ресурсами в контексте Future
     *
     */
    
    extension [A] (future: Future[A]){
      def ensuring(finalizer: Future[Any]): Future[A] = future.transformWith {
        case Failure(exception) => finalizer.flatMap(_ => Future.failed(exception))
        case Success(value) => finalizer.flatMap(_ => Future.successful(value))
      }
    }



    /**
     * Написать код, который получит ресурс, воспользуется им и освободит
     */

    lazy val futureResult = acquireFutureResource.flatMap(r => use(r)
      .ensuring(releaseFutureResource(r)))



  }


  object zioResource{


    /**
     * Реализовать ф-цию, которая будет описывать открытие файла с помощью ZIO эффекта
     */
    def openFile(fileName: String): Task[BufferedSource] = ZIO.attempt(Source.fromFile(fileName)) 

    def openDummyFile(fileName: String) = ZIO.attempt(Resource(fileName))
    /**
     * Реализовать ф-цию, которая будет описывать закрытие файла с помощью ZIO эффекта
     */

    def closeFile(file: Source) = ZIO.attempt(file.close()).orDie

    def closeDummyFile(file: Resource) = ZIO.attempt(file.close()).orDie

    /**
     * Написать эффект, который прочитает строчки из файла и выведет их в консоль
     */

    def handleFile(file: Source) = ZIO.foreach(file.getLines().toList){ str =>
      ZIO.attempt(println(str))
    }

    def handleDummyFile(file: Resource) = ZIO.attempt(traditional.use(file))


    val r: Task[Unit] = ZIO.acquireReleaseWith(openDummyFile("Z file"))(closeDummyFile)(handleDummyFile)

    val r2: ZIO[Scope, Throwable, BufferedSource] = 
      ZIO.fromAutoCloseable(ZIO.attempt(Source.fromFile("test1.txt")))
      
    val r3: ZIO[Scope, Throwable, BufferedSource] = 
      ZIO.attemptBlocking(Source.fromFile("test1.txt")).withFinalizer(bs => 
      ZIO.succeedBlocking(bs.close()))

    /**
     * Написать эффект, который откроет 2 файла, прочитает из них строчки,
     * выведет их в консоль и корректно закроет оба файла
     */
    
    val twoFiles: ZIO[Any, Throwable, List[Unit]] = ZIO.acquireReleaseWith(openFile("test1.txt"))(closeFile){ f1 =>
      ZIO.acquireReleaseWith(openFile("test2.txt"))(closeFile){ f2 =>
        handleFile(f1) *> handleFile(f2)
      }
    }
    
    val twoFilesDummy = ZIO.acquireReleaseWith(openDummyFile("dummy1.txt"))(closeDummyFile){ f1 =>
      ZIO.acquireReleaseWith(openDummyFile("dummy2.txt"))(closeDummyFile){ f2 =>
        handleDummyFile(f1) *> handleDummyFile(f2)
      }
    }
    
  }

}


object zioScope{



  /**
   * Написать эффект открывающий / закрывающий первый файл
   */
  lazy val file1: ZIO[Scope, Throwable, Resource] = 
    ZIO.acquireRelease(openDummyFile("Scoped 1"))(closeDummyFile)

  /** Написать эффект открывающий / закрывающий второй файл
   *
   */
  lazy val file2: ZIO[Scope, Throwable, Resource] = 
    ZIO.acquireRelease(openDummyFile("Scoped 2"))(closeDummyFile)


  /**
   * Использование ресурсов
   */

  lazy val fileCombined: ZIO[Scope, Throwable, (Resource, Resource)] = file1 zip file2

  /**
   * Написать эффект, который воспользуется ф-ей handleFile из блока про bracket
   * для печати строчек в консоль
   */


  val useCombinedResources: ZIO[Scope, Throwable, Unit] = fileCombined.flatMap{case (f1, f2) =>
    handleDummyFile(f1) *> handleDummyFile(f2)
  }

  
  /**
   * Комбинирование ресурсов
   */



  // Комбинирование



  /**
   * Написать эффект, который прочитает и выведет строчки из обоих файлов
   */





  /**
   * Множество ресурсов
   */

  lazy val fileNames: List[String] = List(
    "Scope R1",
    "Scope R2",
    "Scope R3",
    "Scope R4",
    "Scope R5",
    "Scope R6",
    "Scope R7",
    "Scope R8",
    "Scope R9",
    "Scope R10"
  )

  def file(name: String): ZIO[Scope, Throwable, Resource] = ZIO.acquireRelease(openDummyFile(name))(closeDummyFile)

  def file2(name: String) = ???


  // множественное открытие / закрытие
  lazy val files: ZIO[Scope, Throwable, List[Resource]] = ZIO.foreach(fileNames)(file)

  lazy val files2 = ???
  




  // Использование


  // обработать N файлов
  
  val nFilesResult: ZIO[Scope, Throwable, Unit] = files.parallelFinalizers.flatMap{ files =>
    ZIO.foreachPar(files){f =>
      handleDummyFile(f)
    }
  }.unit



  lazy val files3 = ???

  /**
   * Прочитать строчки из файлов и вернуть список этих строк используя files3
   */
  lazy val r3: Task[List[String]] = ???
  

}