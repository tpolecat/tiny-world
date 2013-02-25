package org.tpolecat.tiny.world.example

import org.tpolecat.tiny.world.FactoryWorld
import java.text.DateFormat
import java.util.Date
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

object DecimalFormatWorld extends FactoryWorld {
  protected type State = DecimalFormat
  protected def initialState = new DecimalFormat()
  type Result = Double => String
  protected def finalState(s: State) = { t: Double => s.format(t) }

  val currency: GetterSetter[Currency] =
    getterSetter(_.getCurrency, _.setCurrency(_))

  val decimalSymbols: GetterSetter[DecimalFormatSymbols] =
    getterSetter(_.getDecimalFormatSymbols, _.setDecimalFormatSymbols(_))

  val groupingSize: GetterSetter[Int] =
    getterSetter(_.getGroupingSize, _.setGroupingSize(_))

  val maxDigits: GetterSetter[(Int, Int)] = getterSetter(
    s => (s.getMaximumIntegerDigits, s.getMaximumFractionDigits),
    (s, p) => { s.setMaximumIntegerDigits(p._1); s.setMaximumFractionDigits(p._2) })

  val minDigits: GetterSetter[(Int, Int)] = getterSetter(
    s => (s.getMinimumIntegerDigits, s.getMinimumFractionDigits),
    (s, p) => { s.setMinimumIntegerDigits(p._1); s.setMinimumFractionDigits(p._2) })

  val multiplier: GetterSetter[Int] =
    getterSetter(_.getMultiplier, _.setMultiplier(_))

  val negativeAffix: GetterSetter[(String, String)] = getterSetter(
    s => (s.getNegativePrefix, s.getNegativeSuffix),
    (s, p) => { s.setNegativePrefix(p._1); s.setNegativeSuffix(p._2) })

  object pattern {
    def :=(s: String) = effect(_.applyLocalizedPattern(s))
  }

}

// TODO: convert to DecimalFormat
object NumberFormatWorldTest extends App {

  import DecimalFormatWorld._

  val fact = for {
    _ <- minDigits := (3, 4)
  } yield ()

  val f = fact.unsafeBuild // TODO: must accept locale here

  println(f(0.12))

}