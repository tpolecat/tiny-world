package org.tpolecat.tiny.world

import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.std.tuple._

trait LensLifting { this:World =>

  type StateLens[B] = Lens[State, B]

  object MutableStateLens {
    def apply[B](f: (State, B) => Unit, g: State => B) =
      Lens[State, B](s => Store(b => { (s, b); s }, g(s)))
  }

  implicit class LensLifter[B](lens: StateLens[B]) {
    object lift extends Action[B](w => return_((w, lens.get(w)))) {
      def :=(a: B) = action(s => (s, lens.set(s, a)))
    }
  }

}
