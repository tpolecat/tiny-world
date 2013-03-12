package org.tpolecat.tiny.world.example

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency

import org.tpolecat.tiny.world.Props
import org.tpolecat.tiny.world.World

object DecimalFormatWorld extends World with Props {

  protected type State = DecimalFormat

  val currency: Prop[Currency] = Prop(_.getCurrency, _.setCurrency(_))
  val decimalSymbols: Prop[DecimalFormatSymbols] = Prop(_.getDecimalFormatSymbols, _.setDecimalFormatSymbols(_))
  val groupingSize: Prop[Int] = Prop(_.getGroupingSize, _.setGroupingSize(_))
  val multiplier: Prop[Int] = Prop(_.getMultiplier, _.setMultiplier(_))

  val maxDigits: Prop[(Int, Int)] = Prop(
    s => (s.getMaximumIntegerDigits, s.getMaximumFractionDigits),
    (s, p) => { s.setMaximumIntegerDigits(p._1); s.setMaximumFractionDigits(p._2) })

  val minDigits: Prop[(Int, Int)] = Prop(
    s => (s.getMinimumIntegerDigits, s.getMinimumFractionDigits),
    (s, p) => { s.setMinimumIntegerDigits(p._1); s.setMinimumFractionDigits(p._2) })

  val negativeAffix: Prop[(String, String)] = Prop(
    s => (s.getNegativePrefix, s.getNegativeSuffix),
    (s, p) => { s.setNegativePrefix(p._1); s.setNegativeSuffix(p._2) })

  object pattern {
    def :=(s: String) = effect(_.applyLocalizedPattern(s))
  }

  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeBuild: Double => String = runWorld(a, new DecimalFormat)._1.format
  }

}

object DecimalFormatWorldTest extends App {

  import DecimalFormatWorld._

  val fact = for {
    _ <- minDigits := (3, 4)
  } yield ()

  val f = fact.unsafeBuild // TODO: must accept locale here

  println(f(0.12))

}