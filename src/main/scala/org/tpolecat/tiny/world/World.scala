package org.tpolecat.tiny.world

import scalaz._
import scalaz.Free._
import scalaz.std.function._

trait World {

  protected type State
  
  final class Action[+A] private[World] (private[World] val t: State => Trampoline[(State, A)]) {

    def map[B](f: A => B): Action[B] =
      new Action(w => for { (nw, a) <- t(w) } yield (nw, f(a)))

    def flatMap[B](f: A => Action[B]): Action[B] =
      new Action(w => for { (nw, a) <- t(w); x <- f(a).t(w) } yield x)

  }
  
  protected[World] def runWorld[A](a:Action[A], w: State): (State, A) = a.t(w).run

  implicit def ActionSemigroup[A](implicit A: Semigroup[A]): Semigroup[Action[A]] =
    Monoid.liftSemigroup[Action, A](ActionMonad, A)

  implicit def ActionMonoid[A](implicit A: Monoid[A]): Monoid[Action[A]] =
    Monoid.liftMonoid[Action, A](ActionMonad, A)

  implicit object ActionMonad extends Monad[Action] {
    def point[A](a: => A): Action[A] = action(w => (w, a))
    override def map[A, B](fa: Action[A])(f: (A) => B) = fa map f
    def bind[A, B](fa: Action[A])(f: (A) => Action[B]): Action[B] = fa flatMap f
  }

  // TODO: not happy about names
  
  // Constructs an action that evolves the world state
  protected def action[A](f: State => (State, A)): Action[A] = new Action(w => return_(f(w)))

  // Constructs an action that does not evolve the world state
  protected def effect[A](f: State => A): Action[A] = action(w => (w, f(w)))

  // Constructs an action that does not rely on the world state
  protected def unit[A](a: => A): Action[A] = ActionMonad.point(a)
  
}

trait PrivateWorld extends World {
  protected def initialState:State
  implicit class RunnableAction[A](a:Action[A]) {
    def run:A = runWorld(a, initialState)._2
  }
}

trait PublicWorld extends World {
  type State // public now
  implicit class RunnableAction[A](a:Action[A]) {
    def run(w:State):A = eval(w)._2
    def eval(w:State):(State, A) = runWorld(a, w)
  }
}


