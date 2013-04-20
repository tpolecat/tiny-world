package org.tpolecat.tiny.world

import language.higherKinds
import scalaz.Monad

trait EffectWorld {

  /** Our world's state. Subclasses may choose to make this type public. */
  protected type State

  /** An action in this `World`, returning a result of type `A`. */
  type Action[A]
  
  /** Run an `Action` with some initial `State`, returning the final state and result. */
  protected def runWorld[A](a: Action[A], w: State): (State, A)

  /** Construct an `Action` for a computation that transitions the `State` and produces a result of type `A`. */
  protected def action[A](f: State => (State, A)): Action[A]

  /** Construct an `Action` for a computation consumes the `State` and produces a result of type `A`. */
  protected final def effect[A](f: State => A): Action[A] = action(s => (s, f(s)))

  /** Construct an `Action` for a computation that simply returns a value of type `A`. */
  protected final def unit[A](a: => A): Action[A] = action(s => (s, a))

}


