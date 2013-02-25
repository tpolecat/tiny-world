package org.tpolecat.tiny.world

import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.std.tuple._

trait World {

  protected type State

  sealed class Action[+A] private[World] (private[World] val t: State => Trampoline[(State, A)]) {

    def map[B](f: A => B): Action[B] =
      new Action(w => for { (nw, a) <- t(w) } yield (nw, f(a)))

    def flatMap[B](f: A => Action[B]): Action[B] =
      new Action(w => for { (nw, a) <- t(w); x <- f(a).t(w) } yield x)

  }

  protected[World] def runWorld[A](a: Action[A], w: State): (State, A) = a.t(w).run

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

  class GetterSetter[A](get: State => A, set: (State, A) => Unit)
    extends Action[A](w => return_((w, get(w)))) {
    def :=(a: A) = action(s => (s, set(s, a)))
  }

  protected def getterSetter[A](get: State => A, set: (State, A) => Unit) =
    new GetterSetter(get, set)

}

trait PrivateWorld extends World {
  protected def initialState: State
  implicit class RunnableAction[A](a: Action[A]) {
    def run: A = runWorld(a, initialState)._2
  }
}

trait ImpurePrivateWorld extends World {
  protected def initialState: State
  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeRun: A = runWorld(a, initialState)._2
  }
}

trait PublicWorld extends World {
  type State // public now
  implicit class RunnableAction[A](a: Action[A]) {
    def run(w: State): A = eval(w)._2
    def eval(w: State): (State, A) = runWorld(a, w)
  }
}

trait ImpureFactoryWorld extends World {
  type State // public now
  protected def initialState: State
  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeBuild: State = unsafeEval._1
    def unsafeEval: (State, A) = runWorld(a, initialState)
  }
}

trait FactoryWorld extends World {
  type Result
  protected def initialState: State
  protected def finalState(s:State):Result
  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeBuild: Result = unsafeEval._1
    def unsafeEval: (Result, A) = {
      val (s, r) = runWorld(a, initialState) 
      (finalState(s), r)
    }
  }

}



