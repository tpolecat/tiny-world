package org.tpolecat.tiny.world.example.cal

import scalaz.{ Enum, Ordering }
import java.util.Calendar._

sealed abstract class Month private (val ord: Int)

object Month {

  case object Jan extends Month(JANUARY)
  case object Feb extends Month(FEBRUARY)
  case object Mar extends Month(MARCH)
  case object Apr extends Month(APRIL)
  case object May extends Month(MAY)
  case object Jun extends Month(JUNE)
  case object Jul extends Month(JULY)
  case object Aug extends Month(AUGUST)
  case object Sep extends Month(SEPTEMBER)
  case object Oct extends Month(OCTOBER)
  case object Nov extends Month(NOVEMBER)
  case object Dec extends Month(DECEMBER)

  private val instances: List[Month] =
    List(Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec)

  def forOrdinal(n: Int): Option[Month] = instances.find(_.ord == n)

  implicit def enum = new Enum[Month] {
    def pred(a: Month): Month = forOrdinal((a.ord - 1) % 12).get
    def succ(a: Month): Month = forOrdinal((a.ord + 1) % 12).get
    def order(x: Month, y: Month): Ordering = Ordering.fromInt(x.ord - y.ord)
    override def succn(n: Int, a: Month) = super.succn(n % 12, a)
    override def predn(n: Int, a: Month) = super.predn(n % 12, a)
    override def min = Some(Jan)
    override def max = Some(Dec)
  }

}

