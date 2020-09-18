import scala.io._
import zio._
import zio.duration._
import console._
import java.io.IOException
import java.io.EOFException
import sttp.client._

/** Yes, it's really Hello, world! in ZIO.
  */
object Hello extends App {

  override def run(args: List[String]): ZIO[Console, Nothing, ExitCode] = {
    putStrLn("Hello, world!").exitCode
  }

}

/** Another really trivial example. PROGRAMS ARE VALUE!
 */
object Prompt extends App {

  val program: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStr("What's your name?:  ")
      n <- getStrLn
      _ <- putStrLn(s"Hello, $n!")
    } yield ()

  override def run(args: List[String]): ZIO[Console, Nothing, ExitCode] =
    program.exitCode

}

/** Catch all failures.
  */
object Prompt2 extends App {

  val program: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStr("What's your name?:  ")
      n <- getStrLn
      _ <- putStrLn(s"Hello, $n!")
    } yield ()

  override def run(args: List[String]): ZIO[Console, Nothing, ExitCode] =
    program
      .catchAll { e => putStrLn("") *> putStrLn(s"Input failed due to '${e.getMessage}'") }
      .exitCode

}

/** Loop until EOF
  */
object PromptRepeat extends App {

  val program: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStr("What's your name?:  ")
      n <- getStrLn
      _ <- putStrLn(s"Hello, $n!")
    } yield ()

  override def run(args: List[String]): ZIO[Console, Nothing, ExitCode] =
    program
      .forever
      .catchSome { case _: EOFException =>  putStrLn("") *> putStrLn("Bye")}
      .exitCode

}

/** zip and zipPar
  */
object CombinatorExamples {
  val a: ZIO[Console, Nothing, String] = putStrLn("A").as("A")
  val b: ZIO[Console, Nothing, Int]    = putStrLn("1").as(1)
  val f: ZIO[Any, String, Nothing]     = ZIO.fail("Bang!")

  val aZipB: ZIO[Console, Nothing, (String, Int)]       = a <*> b
  val aZipBLeft: ZIO[Console, Nothing, String]          = a <*  b
  val aZipBRight: ZIO[Console, Nothing, Int]            = a  *> b
  val aZipFail: ZIO[Console, String, (String, Nothing)] = a <*> f
  val failZipB: ZIO[Console, String, (Nothing, Int)]    = f <*> b

  val aParB: ZIO[Console, Nothing, (String, Int)]       = a <&> b
  val aParBLeft: ZIO[Console, Nothing, String]          = a <&  b
  val aParBRight: ZIO[Console, Nothing, Int]            = a  &> b
  val aParFail: ZIO[Console, String, (String, Nothing)] = a <&> f
  val failParB: ZIO[Console, String, (Nothing, Int)]    = f <&> b
}

/** A few more ways of handling errors.
  */
object HandlingErrors {
  val a: ZIO[Any, Throwable, Int] = ZIO.fail(new IOException("Bang!"))

  val h1: UIO[Int] = a.catchAll(_ => ZIO.succeed(42))
  val h2: RIO[Console, Int] = a.catchSome { case e: IOException => console.putStrLn(s"${e.getMessage}") *> IO.succeed(42) }
  val h3: UIO[Int] = a.orElse { ZIO.succeed(42) }
  val h4: IO[String, Int] = a.mapError { _.getMessage }
  val h5: UIO[Either[Throwable, Int]] = a.either
  val h6: UIO[Int] = a.eventually   // Not recommended.  :-)
}

/** Schedule examples
  */
object RetryRepeat {
  val a = ZIO.effectTotal(util.Random.nextInt(10)).flatMap(n => if(n > 7) ZIO.fail(s"$n > 7") else putStrLn(n.toString))
  val b = clock.instant.flatMap(i => putStrLn(s"$i"))
  val c = clock.instant.flatMap(i => putStrLn(s"$i")) *> ZIO.fail("Bang")

  val limit = Schedule.fixed(1.second) && Schedule.recurs(10)
  val seq = (Schedule.spaced(1.second) && Schedule.recurs(5)) andThen (Schedule.spaced(5.seconds) && Schedule.recurs(5))
  val expMax = (Schedule.exponential(500.millis) || Schedule.spaced(5.seconds)) && Schedule.recurs(10)

  val fbOne = (Schedule.exponential(500.millis) || Schedule.spaced(5.seconds))
  val fbTwo = (Schedule.spaced(1.minute))
  val fbThree = (Schedule.spaced(1.hour))
  val fallback = (fbOne >>> Schedule.elapsed).whileOutput(_ < 1.minute) andThen
    (fbTwo >>> Schedule.elapsed.whileOutput(_ < 1.hour)) andThen
    (fbThree >>> Schedule.elapsed).whileOutput(_ < 1.day)
}

/** A few ways of creating ZIO instances
  */
object CreatingIOs {
  import argonaut._
  import Argonaut._

  val a: Task[BufferedSource] = ZIO.effect { Source.fromFile("/tmp/fubar") }
  def parseJSON(s: String): IO[String, Json] = { ZIO.fromEither(s.parse) }

  implicit val backend = HttpURLConnectionBackend()
  val h: IO[String, String] =
    ZIO.fromEither{
      basicRequest
        .get(uri"http://iscaliforniaonfire.com").send()
        .body
    }

  // ZIO.fromOption
  // ZIO.fromTry
  // ZIO.fromFuture
}

/** Fork/Join examples.
  */
object ForkJoin {
  val z1 = (ZIO.sleep(1.second) *> putStrLn("A")).as("A").ensuring(putStrLn("A:  cleanup"))
  val z2 = (ZIO.sleep(2.seconds) *> putStrLn("B")).as("B").ensuring(putStrLn("B:  cleanup"))
  val z3 = (ZIO.sleep(3.seconds) *> putStrLn("C")).as("C").ensuring(putStrLn("C:  cleanup"))
  val f  = (ZIO.sleep(1500.millis) *> ZIO.fail("Bang!")).ensuring(putStrLn("F:  cleanup"))

  val t1 =
    for {
      f1 <- z1.fork
      f2 <- z2.fork
      f3 <- z3.fork
      _  <- Fiber.joinAll(List(f1, f2, f3))
    } yield ()

  val t2 =
    for {
      f1 <- z1.fork
      f2 <- z2.fork
      f3 <- z3.fork
      f4 <- f.fork
      _  <- Fiber.joinAll(List(f1, f2, f3, f4))
    } yield ()

  val t3 =
    for {
      f1 <- z1.fork
      f2 <- z2.fork
      f3 <- z3.fork
      f4 <- f.fork
      _  <- Fiber.awaitAll(List(f1, f2, f3, f4))
    } yield ()

  val t4 =
    for {
      f1 <- z1.fork
      f2 <- z2.fork
      f3 <- z3.fork
      r  <- Fiber.collectAll(List(f1, f2, f3)).await
    } yield r

  val t5 =
    for {
      f1 <- z1.fork
      f2 <- z2.fork
      f3 <- z3.fork
      f4 <- f.fork
      r  <- Fiber.collectAll(List(f1, f2, f3, f4)).await
    } yield r
}
