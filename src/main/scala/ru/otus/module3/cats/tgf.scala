package ru.otus.module3.cats

import cats.Monad
import cats.effect.{IO, IOApp}
import cats.syntax.all.*

/*
 * ==========================================================
 * 1. DSL файловой системы
 * ==========================================================
 */

trait FileSystem[F[_]]:
  def readFile(path: String): F[String]

/*
 * ==========================================================
 * 2. DSL HTTP-клиента
 * ==========================================================
 */

trait HttpClient[F[_]]:
  def post(url: String, body: String): F[Unit]

/*
 * ==========================================================
 * 3. Бизнес-логика
 * ==========================================================
 *
 * Она не зависит от IO и конкретных реализаций.
 * Ей нужны две способности:
 *
 * - прочитать файл;
 * - отправить HTTP-запрос.
 */

def postFile[F[_]: Monad](
                           path: String,
                           url: String
                         )(
                           using
                           fs: FileSystem[F],
                           http: HttpClient[F]
                         ): F[Unit] =
  for
    fileContent <- fs.readFile(path)
    _           <- http.post(url, fileContent)
  yield ()

/*
 * ==========================================================
 * 4. Live-интерпретатор FileSystem для IO
 * ==========================================================
 */

given FileSystem[IO] with

  override def readFile(path: String): IO[String] =
    IO.println(s"[FileSystem] Читаю файл: $path") *>
      IO.pure(
        s"""
           |{
           |  "name": "John",
           |  "age": 30
           |}
           |""".stripMargin
      )

/*
 * ==========================================================
 * 5. Live-интерпретатор HttpClient для IO
 * ==========================================================
 */

given HttpClient[IO] with

  override def post(
                     url: String,
                     body: String
                   ): IO[Unit] =
    IO.println(
      s"""
         |[HTTP]
         |POST $url
         |
         |BODY:
         |$body
         |""".stripMargin
    )

/*
 * ==========================================================
 * 6. Точка входа
 * ==========================================================
 */

object Resources extends IOApp.Simple:

  override def run: IO[Unit] =
    postFile[IO](
      path = "users.json",
      url = "https://my-server/api/users"
    )