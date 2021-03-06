package org.tpolecat.tiny.world.example.cal

import java.util.Calendar._
import org.tpolecat.tiny.world._
import scalaz.syntax.monad._
import scalaz._
import java.util.TimeZone
import java.util.Locale
import java.util.GregorianCalendar
import scalaz.effect.IO

/**
 * An `EffectWorld` that provides a pure AIP for date calculations. The implementation is incomplete.
 */
object GregorianCalendarWorld extends World with Props {

  protected type State = GregorianCalendar

  private def genField[A](field: Int, f: Int => A, g: A => Int): Prop[A] =
    Prop(s => f(s.get(field)), (s, a) => s.set(field, g(a)))

  private def intField(f: Int) = genField[Int](f, identity, identity)

  def millisecond = intField(MILLISECOND)
  def second = intField(SECOND)
  def minute = intField(MINUTE)
  def hour = intField(HOUR)
  def hourOfDay = intField(HOUR_OF_DAY)
  def dayOfWeek = genField[Weekday](DAY_OF_WEEK, Weekday.forOrdinal(_).get, _.ord)
  def dayOfMonth = intField(DAY_OF_MONTH)
  def dayOfYear = intField(DAY_OF_YEAR)
  def month = genField[Month](MONTH, Month.forOrdinal(_).get, _.ord)
  def year = intField(YEAR)

  object Env {
    def timeZone: IO[TimeZone] = IO(TimeZone.getDefault)
    def locale: IO[Locale] = IO(Locale.getDefault)
    def timeMillis: IO[Long] = IO(System.currentTimeMillis)
  }

  implicit class RunnableAction[A](a: Action[A]) {

    /**
     * Run this `Action` in the given `TimeZone` and `Locale`, starting at the given time. This is a pure function.
     */
    def run(tz: TimeZone, loc: Locale, time: Long): A = {
      val c = new GregorianCalendar(tz, loc)
      c.setTimeInMillis(time)
      runWorld(a, c)._2
    }

    /**
     * Run this `Action` with the current `TimeZone`, `Locale`, and time. This is a pure function that constructs an
     * `IO` action that can be executed (impurely) via `unsafePerformIO`.
     */
    def run: IO[A] =
      (Env.timeZone |@| Env.locale |@| Env.timeMillis)(run)

  }

}

object CalTest extends App {

  import GregorianCalendarWorld._

  val daysUntilChristmas = for {
    d <- dayOfYear
    _ <- month := Month.Dec
    _ <- dayOfMonth := 25
    c <- dayOfYear
  } yield c - d

  println(daysUntilChristmas.run.unsafePerformIO)

  val now = ((year: Action[Int]) |@| month |@| dayOfMonth).tupled

  println(now.run.unsafePerformIO)

}

