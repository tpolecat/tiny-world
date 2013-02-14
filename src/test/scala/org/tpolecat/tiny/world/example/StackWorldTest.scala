package org.tpolecat.tiny.world.example

import org.tpolecat.tiny.world.StackWorld

object StackWorldTest extends App {

  class MapWorld[K, V] extends StackWorld {
    type State = collection.mutable.Map[K, V]
    def get(k: K): Action[Option[V]] = effect(_.get(k))
    def put(k: K, v: V): Action[Unit] = effect(_.put(k, v))
  }

  object StringIntMapWorld extends MapWorld[String, Int]
  import StringIntMapWorld._

  val action = for {
    a <- get("foo")
    _ <- put("bar", 3)
    _ <- put("foo", 5)
  } yield a

  println(action.eval(collection.mutable.Map("foo" -> 33)))
  
}


