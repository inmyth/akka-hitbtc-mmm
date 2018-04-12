package com.mbcu.hitbtc.mmm.sequences

import java.math.MathContext

import com.mbcu.hitbtc.mmm.models.request.{NewOrder, NewOrderParam}
import com.mbcu.hitbtc.mmm.models.response.Side
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.sequences.Strategy.Movement.Movement
import com.mbcu.hitbtc.mmm.sequences.Strategy.Strategies.Strategies
import com.mbcu.hitbtc.mmm.utils.MyUtils
import play.api.libs.json.{Reads, Writes}

import scala.math.BigDecimal.RoundingMode

object Strategy {
  val mc : MathContext = MathContext.DECIMAL64
  val ZERO = BigDecimal("0")
  val ONE = BigDecimal("1")
  val CENT = BigDecimal("100")
  val INFINITY = BigDecimal("1e60")

  object Movement extends Enumeration {
    type Movement = Value
    val UP, DOWN  = Value
  }

  object Strategies extends Enumeration {
    type Strategies = Value
    val ppt, fullfixed = Value

    implicit val strategiesReads = Reads.enumNameReads(Strategies)
    implicit val strategiesWrites = Writes.enumNameWrites
  }

  def seed (qty0 : BigDecimal, unitPrice0 : BigDecimal, amtPwr : Int,
            ctrScale : Int, basScale : Int, symbol : String, levels : Int, gridSpace : BigDecimal,
            side: Side, isPulledFromOtherSide : Boolean, strategy : Strategies, isNoQtyCutoff : Boolean,
            maxPrice : Option[BigDecimal] = None, minPrice : Option[BigDecimal] = None) : Seq[NewOrder] = {

    val range = strategy match {
      case Strategies.ppt => if(isPulledFromOtherSide) 2 else 1
      case Strategies.fullfixed => if(isPulledFromOtherSide) 2 else 1
      case _ => 0
    }

    (range until (levels + range))
      .map(n => {
        val movement = if (side == Side.buy) Movement.DOWN else Movement.UP
        strategy match {
          case Strategies.ppt =>
            val mtp = ONE + gridSpace(mc) / CENT
            List.fill(n)(1)
                .foldLeft(unitPrice0, qty0)((z, i) => ppt(z._1, z._2, amtPwr, mtp, ctrScale, movement))

          case Strategies.fullfixed =>
            List.fill(n)(1)
              .foldLeft(unitPrice0, qty0)((z, i) => step(z._1, z._2, gridSpace, ctrScale, movement))

          case _ => (ZERO, ZERO)
        }
        }
      )
      .filter(_._1 > 0)
      .map(p1q1 => if (p1q1._2 <= 0 && isNoQtyCutoff) (p1q1._1, calcMinAmount(ctrScale)) else p1q1)
      .filter(_._2 > 0)
      .filter(minPrice match {
        case Some(mp) => _._1 >= mp
        case _ => _._1 > ZERO
      })
      .filter(maxPrice match {
        case Some(mp) => _._1 <= mp
        case None => _._1 < INFINITY
      })
      .map(p1q1 => {
        val newOrderParam = NewOrderParam(MyUtils.clientOrderId(symbol, side), symbol, side, p1q1._1.setScale(basScale, RoundingMode.HALF_EVEN), p1q1._2)
        NewOrder(newOrderParam.clientOrderId, newOrderParam)
      })
  }


  def counter(qty0 : BigDecimal, unitPrice0 : BigDecimal, amtPwr : Int, ctrScale : Int, basScale : Int, symbol : String, gridSpace : BigDecimal, side : Side,
              strategy : Strategies, isNoQtyCutoff : Boolean,
              maxPrice : Option[BigDecimal] = None, minPrice : Option[BigDecimal] = None)
  : Seq[NewOrder] = {
    val newSide = if (side == Side.buy) Side.sell else Side.buy
    seed(qty0, unitPrice0, amtPwr, ctrScale, basScale, symbol, 1, gridSpace, newSide, isPulledFromOtherSide = false, strategy, isNoQtyCutoff, maxPrice, minPrice)
  }

  def ppt(unitPrice0 : BigDecimal, qty0 : BigDecimal, amtPower : Int, rate : BigDecimal, ctrScale : Int, movement: Movement): (BigDecimal, BigDecimal) ={
    val unitPrice1 = if (movement == Movement.DOWN) unitPrice0(mc) / rate else unitPrice0 * rate
    val mtpBoost = MyUtils sqrt rate pow amtPower
    var qty1 = if (movement == Movement.DOWN) MyUtils.roundCeil(qty0 * mtpBoost, ctrScale) else MyUtils.roundFloor(qty0(mc) / mtpBoost, ctrScale)
    (unitPrice1, qty1)
  }

  def step(unitPrice0 : BigDecimal, qty0 : BigDecimal, rate : BigDecimal, ctrScale : Int, movement: Movement ): (BigDecimal, BigDecimal) ={
    val unitPrice1 = if (movement == Movement.DOWN) unitPrice0(mc) - rate else unitPrice0 + rate
    val qty1 = qty0
    (unitPrice1, qty1)
  }

  def calcMinAmount(ctrScale : Int) : BigDecimal = {
    ONE.setScale(ctrScale, BigDecimal.RoundingMode.CEILING)
  }

  def calcMid(unitPrice0 : BigDecimal, qty0 : BigDecimal, amtPower : Int, gridSpace : BigDecimal, ctrScale : Int, side: Side, marketMidPrice : BigDecimal, strategy : Strategies)
  : (BigDecimal, BigDecimal, Int) = {
    var base = (unitPrice0, qty0)
    var levels = 0
    val mtp = ONE + gridSpace(mc) / CENT

    side match {
      case Side.buy =>
        while (base._1 < marketMidPrice) {
          levels = levels + 1
          strategy match  {
            case Strategies.ppt => base = ppt(base._1, base._2, amtPower, mtp, ctrScale, Movement.UP)
            case _ => base = step(base._1, base._2, gridSpace, ctrScale, Movement.UP)
          }
        }
      case _ =>
        while (base._1 > marketMidPrice) {
          levels = levels + 1
          strategy match  {
            case Strategies.ppt => base = ppt(base._1, base._2, amtPower, mtp, ctrScale, Movement.DOWN)
            case _ => base = step(base._1, base._2, gridSpace, ctrScale, Movement.DOWN)
          }
        }
    }
    (base._1, base._2, levels)
  }

}
