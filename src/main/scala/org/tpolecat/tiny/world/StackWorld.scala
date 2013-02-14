package org.tpolecat.tiny.world

import scalaz.Monad

/**
 * A simpler kind of world that doesn't use the free monad, which means it can
 * blow the stack.
 */
trait StackWorld {

  type State

  final case class Action[A] private[StackWorld] (eval: State => (State, A)) {
    def map[B](f: A => B): Action[B] = flatMap(s => unit(f(s)))
    def flatMap[B](f: A => Action[B]): Action[B] =
      new Action[B](s => {
        val (s0, a) = eval(s)
        f(a).eval(s0)
      })
  }

  implicit object MonadAction extends Monad[Action] {
    def bind[A, B](fa: Action[A])(f: A => Action[B]): Action[B] = fa.flatMap(f)
    def point[A](a: => A): Action[A] = unit(a)
  }

  protected def action[A](f: State => (State, A)) = new Action(f)
  protected def effect[A](f: State => A): Action[A] = action(s => (s, f(s)))
  protected def unit[A](a: => A): Action[A] = action((_, a))

}

