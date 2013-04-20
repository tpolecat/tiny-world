package org.tpolecat.tiny.world.example

import language.higherKinds
import org.tpolecat.tiny.world.World
import scalaz.effect.IO
import scalaz.effect.IO._

object IOWorld extends World {

  type State = Int

  def mod(f: Int => Int): Action[Unit] = action(s => (f(s), ()))
  def get: Action[Int] = action(s => (s, s))

  def getRandom(n: Int): IOAction[Int] = IO(util.Random.nextInt(n)).liftIO[IOAction]

  implicit class RunnableActionT[M[+_], A](at: ActionT[M, A]) {
    def run(n: Int) = runWorld(at, n)
  }

}

object IOWorldTest extends App {

  import IOWorld._

  val a: IOAction[String] = for {
    n <- get.lift
    _ <- putStrLn("state is now " + n).liftIO[IOAction]
    r <- getRandom(10)
    _ <- putStrLn("multiplying by random " + r).liftIO[IOAction]
    _ <- mod(_ * r).lift
    m <- get.lift
  } yield "state changed to " + m

  val b = for {
    s1 <- a
    s2 <- a
  } yield (s1, s2)

  val io = b.run(1)
  println("action was 'run' with no side effects.")

  println(io.unsafePerformIO)
  println(io.unsafePerformIO)

}