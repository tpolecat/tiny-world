package org.tpolecat.tiny.world

import scalaz.Monad

/**
 * A simpler kind of world that doesn't use the free monad, which means it can
 * blow the stack.
 */
trait StackWorld {

  type State

  final case class Action[A] private[StackWorld] (eval: State => (State, A)) {

    def flatMap[B](f: A => Action[B]): Action[B] =
      Action { s =>
        val (s0, a) = eval(s)
        f(a).eval(s0)
      }

    def map[B](f: A => B): Action[B] = flatMap(s => unit(f(s)))

  }

  implicit object MonadAction extends Monad[Action] {
    def bind[A, B](fa: Action[A])(f: A => Action[B]): Action[B] = fa.flatMap(f)
    def point[A](a: => A): Action[A] = unit(a)
  }

  protected def effect[A](f: State => A): Action[A] = Action(s => (s, f(s)))
  protected def unit[A](a: => A): Action[A] = Action((_, a))

}

