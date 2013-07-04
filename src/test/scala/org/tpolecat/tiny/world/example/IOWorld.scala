package org.tpolecat.tiny.world.example

import language.higherKinds
import org.tpolecat.tiny.world.TWorld
import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._
import util.Random.nextInt

object IntWorld extends TWorld {

  protected type State = Int

  /** A module of actions lifted into the specified Applicative (usually a Monad). */
  class Actions[F[+_]: Applicative] extends Lifted[F] {

    def mod(f: Int => Int): Action[Unit] = action(s => (f(s), ()))
    def get: Action[Int] = action(s => (s, s))

    def getRandom(n: Int)(implicit ev: MonadIO[Action]): Action[Int] =
      IO(nextInt(n)).liftIO[Action]

    implicit class Ops[A](a: Action[A]) {
      def run(n: Int) = eval(a, n)
    }

  }

  /** Constructs a set of pre-lifted actions. */
  def lift[F[+_]: Applicative] = new Actions[F]

}

object IOWorldTest extends App {

  val actions = IntWorld.lift[IO]
  import actions._

  val a: Action[String] = for {
    n <- get
    _ <- putStrLn("state is now " + n).liftIO[Action]
    r <- getRandom(10).map(_ + 2)
    _ <- putStrLn("multiplying by random " + r).liftIO[Action]
    _ <- mod(_ * r)
    m <- get
  } yield "state changed to " + m

  val b = for {
    s1 <- a
    s2 <- a
    s3 <- a
  } yield (s1, s2, s3)

  val io = b.run(1)
  println("action was 'run' with no side effects.")

  println(io)
  println(io.unsafePerformIO)

}

