package org.tpolecat.tiny.world

import scala.language.higherKinds
import scala.annotation.tailrec
import scalaz._
import scalaz.Free._

/**
 * An `EffectWorld` implemented directly via the `Free` monad. This is functionally equivalent to the `Trampoline`
 * world but is somewhat lower level and might be useful for future exploration. 
 */
trait FreeWorld extends EffectWorld {

  /** Our operations are simple state transitions. */
  case class Op[+A] private[FreeWorld] (f: State => (State, A))

  /** Operation must have a `Functor` in order to gain a `Free` monad. */
  implicit val OpFunctor: Functor[Op] = new Functor[Op] {
    def map[A, B](op: Op[A])(g: A => B) = Op { s =>
      val (s0, a) = op.f(s)
      (s0, g(a))
    }
  }

  type Action[A] = Free[Op, A]
  val ActionMonad = Monad[Action] // we get this for free

  def action[A](f: State => (State, A)): Action[A] = Suspend(Op(s => {
    val (s0, a) = f(s)
    (s0, Return(a))
  }))

  @tailrec protected final def runWorld[A](a: Action[A], s: State): (State, A) =
    a.resume match { // N.B. resume.fold() doesn't permit TCO
      case -\/(Op(f)) =>
        val (s0, a) = f(s)
        runWorld(a, s0)
      case \/-(a) => (s, a)
    }

}

