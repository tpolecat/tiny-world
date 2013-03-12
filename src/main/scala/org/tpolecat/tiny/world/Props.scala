package org.tpolecat.tiny.world

import language.implicitConversions

import scalaz._
import scalaz.Free._
import scalaz.std.function._
import scalaz.std.tuple._

/** Simple mixin to turn getter/setter pairs into action pairs. */
trait Props { this: EffectWorld =>

  case class Prop[A](get: State => A, set: (State, A) => Unit)

  implicit def Prop2Get[A](p:Prop[A]) = effect(p.get)
  implicit class Prop2Set[A](p:Prop[A]) {
    def :=(a:A) = effect(p.set(_, a))
  }
  
}
