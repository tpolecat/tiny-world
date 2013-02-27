package org.tpolecat.tiny.world.example.cal

import scalaz.{ Enum, Ordering }
import java.util.Calendar._

sealed abstract class Weekday private (val ord: Int)

object Weekday {

  case object Sun extends Weekday(SUNDAY)
  case object Mon extends Weekday(MONDAY)
  case object Tue extends Weekday(TUESDAY)
  case object Wed extends Weekday(WEDNESDAY)
  case object Thu extends Weekday(THURSDAY)
  case object Fri extends Weekday(FRIDAY)
  case object Sat extends Weekday(SATURDAY)

  private val instances: List[Weekday] =
    List(Sun, Mon, Tue, Wed, Thu, Fri, Sat)

  def forOrdinal(n: Int): Option[Weekday] = instances.find(_.ord == n)

  implicit def enum = new Enum[Weekday] {
    def pred(a: Weekday): Weekday = forOrdinal((a.ord - 1) % 7).get
    def succ(a: Weekday): Weekday = forOrdinal((a.ord + 1) % 7).get
    def order(x: Weekday, y: Weekday): Ordering = Ordering.fromInt(x.ord - y.ord)
    override def succn(n: Int, a: Weekday) = super.succn(n % 7, a)
    override def predn(n: Int, a: Weekday) = super.predn(n % 7, a)
    override def min = Some(Sun)
    override def max = Some(Sat)
  }

}

