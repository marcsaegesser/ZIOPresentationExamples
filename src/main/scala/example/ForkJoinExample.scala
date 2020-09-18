package example

import zio._
import zio.blocking._
import zio.clock._
import zio.console._
import zio.duration._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.CORS
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder
import zio.interop.catz._
import cats.effect.{ExitCode => CatsExitCode}

/** Demonstrate fork/join and interruption.
  *
  * App installs a JVM shutdown hook that will interrupt the main
  * fiber. Use Ctrl-c to exit which will result in all fibers being
  * interrupted and cleaned up.
  */
object ForkJoinExample extends App {
  import DataLoop._

  type HttpEnvironment = Clock
  type HttpTask[A] = RIO[HttpEnvironment, A]

  val program =
    for {
      _       <- putStrLn("ForJoinExample started")
      data    <- dataLoop.fork                                           // Start the dataLoop
      _       <- putStrLn("Try http://localhost/example in a browser")
      httpApp =  Router[HttpTask]("example" -> routes).orNotFound        // Create an HTTP application for the given prefix
      http    <- ZIO.runtime[HttpEnvironment].flatMap { implicit rts =>  // Start the HTTP server
        BlazeServerBuilder[HttpTask]
          .bindHttp(8080, "localhost")
          .withHttpApp(CORS(httpApp))
          .serve
          .compile[HttpTask, HttpTask, CatsExitCode]
          .drain
      }.fork
      _  <- putStrLn("Use Ctrl-c to terminate.")
      _  <- Fiber.joinAll(List(data, http))
    } yield ()

  val dsl: Http4sDsl[HttpTask] = Http4sDsl[HttpTask]      // Instantiate an Http4s DSL for our task type
  import dsl._

  def routes =
    HttpRoutes.of[HttpTask] {                             // Define the routes for our web app
      case GET -> Root            => Ok("Hello, world!")
    }

  override def run(ags: List[String]) = {
    program.exitCode
  }
}

/** A silly example of a basic data processing loop.
  */
object DataLoop {

  import DataSource._

  case class Widget(x: String)

  val dataLoop =
    ZIO.bracket(DataSource())(s => (s.close() <* putStrLn("DataLoop Released")).ignore) { source =>
      (for {
        ds <- source.fetch(5.seconds)
        ws <- mkWidgets(ds)
        _  <- (publishWidgets(ws) <* source.commit()).uninterruptible
        _  <- putStrLn("dataLoop iteration complete")
      } yield ()).repeat(Schedule.forever)
    }

  def mkWidgets(data: List[RawData]): ZIO[Any, Throwable, List[Widget]] =
    ZIO.effect(data.map(r => Widget(r.i.toString)))


  def publishWidgets(ws: List[Widget]): ZIO[Any, Throwable, Int] =
    ZIO.effect(ws.size)
}

/** Doesn't do anything but demonstrate canceling a blocking effect.
  */
object DataSource {
  case class RawData(i: Int)

  trait Service {
    def close(): Task[Unit]
    def fetch(duration: Duration): ZIO[Blocking, Throwable, List[RawData]]
    def commit(): ZIO[Any, Throwable, Unit]
    def wakeup: Task[Unit]
  }

  def apply() =
    ZIO.effect {
      new Service {
        import java.util.concurrent._

        val queue = new SynchronousQueue[List[Int]]()

        def close(): Task[Unit] = ZIO.unit

        def fetch(duration: Duration): ZIO[Blocking, Throwable, List[RawData]] =
          effectBlockingCancelable {
            val data = Option(queue.poll(duration.toMillis(), TimeUnit.MILLISECONDS))
            data.map(_.map(RawData)).getOrElse(List.empty[RawData])
          } (wakeup.ignore)

        def commit(): ZIO[Any, Throwable, Unit] = ZIO.unit

        def wakeup: Task[Unit] =
          ZIO.effect(queue.put(List.empty[Int]))
      }
    }
}
