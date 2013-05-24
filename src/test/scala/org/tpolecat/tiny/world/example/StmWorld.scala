package org.tpolecat.tiny.world.example

import scala.concurrent.stm.InTxn
import scala.concurrent.stm.Ref
import scala.concurrent.stm.atomic
import scala.language.implicitConversions

import org.tpolecat.tiny.world.World

import scalaz.Scalaz._
import scalaz.concurrent.Promise
import scalaz.effect.IO

// This file demonstrates a way to hide STM references in an effect world that hides all
// aspects of STM-ness while retaining transactional composability. We implement two worlds
// with the same interface, one with STM and one without and compare results of access by
// multiple threads.
//
// Sample run:
//
// Running with FailWorld
// Final values per task: 351, 313, 494, 371, 493
// Final value: 494
//
// Running with STM World
// Final values per task: 637, 847, 736, 854, 1000
// Final value: 1000

/** Interface for a world with a single Int as internal mutable state. */
abstract class MutableStateWorld(val name: String) extends World {

  // Actions
  def take: Action[Int]
  def put(n: Int): Action[Unit]

  // Actions should be runnable. The result is an IO effect.
  trait RunnableAction[A] {
    def run: IO[A]
  }

  // And this should be implicit
  implicit def runnable[A](a: Action[A]): RunnableAction[A]

}

/** A `MutableStateWorld` that uses STM to provide transactional actions. */
class STMWorld(initial: Int) extends MutableStateWorld("STM World") {

  // Actions pass a transaction as their state
  protected type State = InTxn

  // Our mutable state is an STM ref.
  private val num: Ref[Int] = Ref(initial)

  // Actions are effects that consume the transaction.
  def take: Action[Int] = effect { implicit tx => num() }
  def put(n: Int): Action[Unit] = effect { implicit tx => num() = n }

  // Running an action results in an atomic transaction in IO
  implicit def runnable[A](a: Action[A]): RunnableAction[A] =
    new RunnableAction[A] {
      def run: IO[A] = IO(atomic(runWorld(a, _)._2))
    }

}

/** A `MutableStateWorld` with no synchronization logic. */
class FailWorld(initial: Int) extends MutableStateWorld("FailWorld") {

  // No passed state
  protected type State = Unit

  // Our mutable state is a bare int.
  private var num: Int = initial;

  // Actions are trivial.
  def take: Action[Int] = unit(num)
  def put(n: Int): Action[Unit] = unit(num = n)

  // To run we simply wrap in IO
  implicit def runnable[A](a: Action[A]): RunnableAction[A] =
    new RunnableAction[A] {
      def run: IO[A] = IO(runWorld(a, ())._2)
    }

}

object StmWorldTest extends App {

  // Test with both worlds.
  test(new FailWorld(0))
  test(new STMWorld(0))

  def test(w: MutableStateWorld) {

    import w._

    // An action to increment in two steps, built from the provided primitives.
    // This is safe in STMWorld but will sometimes be interleaved in FailWorld.
    val inc: Action[Unit] =
      for {
        n <- take
        _ <- put(n + 1)
      } yield ()

    // Generic helper, do something N times (is this built into scalaz somewhere?)
    def doTimes(n: Int)(a: IO[Unit]): IO[Unit] =
      if (n == 0) IO() else a >> doTimes(n - 1)(a)

    // IO effect that increments a few times and returns the last value seen
    def incN(n: Int): IO[Int] =
      for {
        _ <- doTimes(n)(inc.run)
        n <- take.run
      } yield n

    // Start several incN tasks running
    val p: Promise[List[Int]] =
      List.fill(5)(Promise(incN(200).unsafePerformIO)).sequence

    // Results
    println("Running with " + w.name)
    println("Final values per task: " + p.get.mkString(", ")) // blocks
    println("Final value: " + p.get.max)
    println()

  }

}





