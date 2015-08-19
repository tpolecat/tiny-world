package org.tpolecat.tiny.world

import language.higherKinds
import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.effect.IO
import scalaz.effect.MonadIO

/** An `EffectWorld` implemented in terms of `Trampoline`. */
trait World extends EffectWorld {

  class Action[+A] protected[World] (private[World] val t: State => Trampoline[(State, A)]) {
    def map[B](f: A => B): Action[B] =
      new Action(w => for { p <- t(w) } yield (p._1, f(p._2)))
    def flatMap[B](f: A => Action[B]): Action[B] =
      new Action(w => for { p <- t(w); x <- f(p._2).t(p._1) } yield x)
  }

  object Action {

    implicit object ActionMonad extends Monad[Action] {
      def point[A](a: => A): Action[A] = 
        action(w => (w, a))      
      override def map[A, B](fa: Action[A])(f: (A) => B) = 
        fa map f      
      def bind[A, B](fa: Action[A])(f: (A) => Action[B]): Action[B] = 
        fa flatMap f
    }

  }

  protected def runWorld[A](a: Action[A], w: State): (State, A) = 
    a.t(w).run

  protected def action[A](f: State => (State, A)): Action[A] = 
    new Action(w => return_(f(w)))

}



