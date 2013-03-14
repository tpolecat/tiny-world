package org.tpolecat.tiny.world

import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.std.tuple._

/**
 * An `EffectWorld` implemented on top of `Free[Function0, A]`, otherwise known as `Trampoline[A]`. The implementation
 * is suspiciously similar to `scalaz.effect.IO`. This is probably the implementation you want to use.
 */
trait World extends EffectWorld {

  class Action[+A] protected[World] (private[World] val t: State => Trampoline[(State, A)]) {

    def map[B](f: A => B): Action[B] =
      new Action(w => for { (nw, a) <- t(w) } yield (nw, f(a)))

    def flatMap[B](f: A => Action[B]): Action[B] =
      new Action(w => for { (nw, a) <- t(w); x <- f(a).t(w) } yield x)

  }

  protected def runWorld[A](a: Action[A], w: State): (State, A) = a.t(w).run

  implicit object ActionMonad extends Monad[Action] {
    def point[A](a: => A): Action[A] = action(w => (w, a))
    override def map[A, B](fa: Action[A])(f: (A) => B) = fa map f
    def bind[A, B](fa: Action[A])(f: (A) => Action[B]): Action[B] = fa flatMap f
  }

  protected def action[A](f: State => (State, A)): Action[A] = new Action(w => return_(f(w)))

}



