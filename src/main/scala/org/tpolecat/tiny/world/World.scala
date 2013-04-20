package org.tpolecat.tiny.world

import language.higherKinds
import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.effect.IO
import scalaz.effect.MonadIO

/**
 * An `EffectWorld` implemented on top of `Free[Function0, A]`, otherwise known as `Trampoline[A]`. The implementation
 * is suspiciously similar to `scalaz.effect.IO`. This is probably the implementation you want to use.
 */
trait World extends EffectWorld {

  // ACTION

  class Action[+A] protected[World] (private[World] val t: State => Trampoline[(State, A)]) {

    def map[B](f: A => B): Action[B] =
      new Action(w => for { p <- t(w) } yield (p._1, f(p._2)))

    def flatMap[B](f: A => Action[B]): Action[B] =
      new Action(w => for { p <- t(w); x <- f(p._2).t(w) } yield x)

    def lift[M[+_]](implicit M: Applicative[M]): ActionT[M, A] =
      new ActionT(s => M.point(runWorld(this, s)))

  }

  object Action extends ActionInstances

  // Implementation of EffectWorld
  protected def runWorld[A](a: Action[A], w: State): (State, A) = a.t(w).run
  protected def action[A](f: State => (State, A)): Action[A] = new Action(w => return_(f(w)))

  // TRANSFORMER

  class ActionT[M[+_], +A](private[World] val run: State => M[(State, A)]) { self =>

    def map[C](f: A => C)(implicit M: Functor[M]): ActionT[M, C] =
      new ActionT(a => M.map(run(a)) { case (s, a) => (s, f(a)) })

    def flatMap[C](f: A => ActionT[M, C])(implicit M: Bind[M]): ActionT[M, C] =
      new ActionT(r => M.bind(run(r)) { case (s, a) => f(a).run(s) })

  }

  object ActionT extends ActionTInstances
  protected def runWorld[M[+_], A](a: ActionT[M, A], w: State): M[(State, A)] = a.run(w)

  trait ActionInstances {

    implicit object ActionMonad extends Monad[Action] {
      def point[A](a: => A): Action[A] = action(w => (w, a))
      override def map[A, B](fa: Action[A])(f: (A) => B) = fa map f
      def bind[A, B](fa: Action[A])(f: (A) => Action[B]): Action[B] = fa flatMap f
    }

  }

  type IOAction[+A] = ActionT[IO, A]

  trait ActionTInstances {

    implicit def MonadActionT[M[+_]](implicit M: Monad[M]) = new Monad[({ type l[a] = ActionT[M, a] })#l] {
      def point[A](a: => A): ActionT[M, A] = new ActionT(s => M.point((s, a)))
      override def map[A, B](fa: ActionT[M, A])(f: (A) => B): ActionT[M, B] = fa map f
      def bind[A, B](fa: ActionT[M, A])(f: (A) => ActionT[M, B]): ActionT[M, B] = fa flatMap f
    }

    implicit object OpMonadIO extends MonadIO[IOAction] {
      def point[A](a: => A): IOAction[A] = MonadActionT[IO].point(a)
      def bind[A, B](fa: IOAction[A])(f: A => IOAction[B]): IOAction[B] = fa.flatMap(f)
      def liftIO[A](ioa: IO[A]): IOAction[A] = new ActionT(s => ioa.map((s, _)))
    }

  }

}



