package org.tpolecat.tiny.world

import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.std.tuple._

trait ImpureState { this: World =>

  class GetterSetter[A](get: State => A, set: (State, A) => Unit)
    extends Action[A](w => return_((w, get(w)))) {
    def :=(a: A) = action(s => (s, set(s, a)))
  }

  protected def getterSetter[A](get: State => A, set: (State, A) => Unit) =
    new GetterSetter(get, set)

}