package org.tpolecat.tiny.world.example

import java.util.Calendar
import org.tpolecat.tiny.world._
import scalaz.syntax.monad._
import scalaz._
import java.util.TimeZone
import java.util.Locale
import java.util.GregorianCalendar

sealed abstract class Month(val ord: Int) {
  Month.add(this)
}

object Month {
  private var instances: Map[Int, Month] = Map()
  private def add(m: Month) = {
    instances.get(m.ord).foreach { m0 =>
      sys.error("Duplicate ordinal: %s ~ %s".format(m0, m))
    }
    instances += m.ord -> m
  }
  def apply(ord: Int) = instances.get(ord)

  implicit def enum = new Enum[Month] {
    def pred(a: Month): Month = Month.apply((a.ord + 11) % 12).get
    def succ(a: Month): Month = Month.apply((a.ord + 1) % 12).get
    def order(x: Month, y: Month): Ordering = Ordering.fromInt(x.ord - y.ord)
    override def succn(n: Int, a: Month) = super.succn(n % 12, a)
    override def predn(n: Int, a: Month) = super.predn(n % 12, a)
    override def min = Some(January)
    override def max = Some(December)
  }

}

case object January extends Month(Calendar.JANUARY)
case object February extends Month(Calendar.FEBRUARY)
case object March extends Month(Calendar.MARCH)
case object April extends Month(Calendar.APRIL)
case object May extends Month(Calendar.MAY)
case object June extends Month(Calendar.JUNE)
case object July extends Month(Calendar.JULY)
case object August extends Month(Calendar.AUGUST)
case object September extends Month(Calendar.SEPTEMBER)
case object October extends Month(Calendar.OCTOBER)
case object November extends Month(Calendar.NOVEMBER)
case object December extends Month(Calendar.DECEMBER)

sealed abstract class Weekday(val n: Int)
case object Sunday extends Weekday(Calendar.SUNDAY)
case object Monday extends Weekday(Calendar.MONDAY)
case object Tuesday extends Weekday(Calendar.TUESDAY)
case object Wednesday extends Weekday(Calendar.WEDNESDAY)
case object Thursday extends Weekday(Calendar.THURSDAY)
case object Friday extends Weekday(Calendar.FRIDAY)
case object Saturday extends Weekday(Calendar.SATURDAY)
object Weekday {
  def apply(n: Int): Option[Weekday] = n match {
    case Calendar.MONDAY => Some(Monday)
    // etc
    case _ => None
  }
}

object GregorianCalendarWorld extends World with Control {

  protected type State = GregorianCalendar

  protected def initialState(tz: TimeZone, loc: Locale, time: Long = 0L) = {
    val c = new GregorianCalendar(tz, loc)
    c.setTimeInMillis(time)
    c
  }

  //  // Private actions
  //  private def add(field: Int): Action[Unit] = effect(_.clear(field))
  //  private def get(field: Int): Action[Int] = effect(_.get(field))
  //  private def add(field: Int, amount: Int): Action[Unit] = effect(_.add(field, amount))
  //  // etc
  //
  //  def clear(): Action[Unit] = effect(_.clear())
  //  def setTimeInMillis(millis: Long): Action[Unit] = effect(_.setTimeInMillis(millis))
  //  def year = get(Calendar.YEAR)
  //  def month = get(Calendar.MONTH).map(_ + 1)
  //  def day = get(Calendar.DAY_OF_MONTH)
  //  def addDay(n: Int) = add(Calendar.DAY_OF_MONTH, n)
  //  def addMonth(n: Int) = add(Calendar.MONTH, n)
  //  // etc

  def day: Action[Weekday] = effect(_.get(Calendar.DAY_OF_WEEK)).map(Weekday(_).get)
  def day(d: Weekday): Action[Unit] = effect(_.set(Calendar.DAY_OF_WEEK, d.n))

  val month = effect(_.get(Calendar.MONTH)).map(Month(_).get)

}

object CalTest extends App {

  import GregorianCalendarWorld._

  val ymd = for {
    d <- day
    _ <- day(Tuesday)
    e <- day
    m <- month
  } yield (m, d, e)

  // This is pure (!)
  //  println(ymd.run)

  //  val ymd2 = (year |@| month |@| day).tupled

  //  println(ymd2.run)

}

