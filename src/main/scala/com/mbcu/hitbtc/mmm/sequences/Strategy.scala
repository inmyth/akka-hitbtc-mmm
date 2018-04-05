package com.mbcu.hitbtc.mmm.sequences

import java.math.MathContext
import java.util.Collections
import java.util.stream.IntStream

import com.mbcu.hitbtc.mmm.models.internal.Bot
import com.mbcu.hitbtc.mmm.models.request.{NewOrder, NewOrderParam}
import com.mbcu.hitbtc.mmm.models.response.{Order, Side}
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.sequences.Strategy.Movement.Movement
import com.mbcu.hitbtc.mmm.sequences.Strategy.Strategies.Strategies
import com.mbcu.hitbtc.mmm.utils.MyUtils
import play.api.libs.json.{Format, Reads, Writes}

import scala.util.Random

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
            qtyScale : Int, symbol : String, levels : Int, gridSpace : BigDecimal,
            side: Side, isPulledFromOtherSide : Boolean, strategy : Strategies,
            maxPrice : Option[BigDecimal] = None, minPrice : Option[BigDecimal] = None) : Seq[NewOrder] = {
    var range = 0

    strategy match {
      case Strategies.ppt => range = if(isPulledFromOtherSide) 3 else 2
      case Strategies.fullfixed => range = if(isPulledFromOtherSide) 2 else 1
      case _ => 0
    }

    (range until (levels + range))
      .map(n => {
        var rate : Option[BigDecimal] = None
        strategy match {
          case Strategies.ppt =>
            val mtp = ONE + gridSpace(mc) / CENT
            rate = Some(Collections.nCopies(n, ONE).stream().reduce((x, y) => x * mtp).get())
          case Strategies.fullfixed => rate = Some(gridSpace * n)
          case _ => rate = None
        }
        rate
      })
      .map(rate => {
        var p1q1 : Option[(BigDecimal, BigDecimal)] = None
        rate match {
          case Some(r) =>
            val movement = if (side == Side.buy) Movement.DOWN else Movement.UP
            strategy match {
              case Strategies.ppt => p1q1 = Some(ppt(unitPrice0, qty0, amtPwr, r, qtyScale, movement))
              case Strategies.fullfixed => p1q1 = Some(step(unitPrice0, qty0, r, qtyScale, movement))
              case _ => p1q1 = None
            }
          case _ => p1q1 = None
        }
        p1q1
      })
      .map(p1q1 => {
        var newOrderParam : Option[NewOrderParam] = None
        p1q1 match {
          case Some(a) => newOrderParam = Some(NewOrderParam(MyUtils.clientOrderId(symbol, side), symbol, side, a._1, a._2))
          case _ => newOrderParam = None
        }
        newOrderParam
      })
      .collect{case Some(p) => p}
      .filter(_.price > ZERO)
      .filter(_.quantity > ZERO)
      .filter(minPrice match {
        case Some(mp) => _.price >= mp
        case _ => _.price > ZERO
      })
      .filter(maxPrice match {
        case Some(mp) => _.price <= mp
        case None => _.price < INFINITY
      })
      .map(p => NewOrder(p.clientOrderId, p))

  }

  def counter(qty0 : BigDecimal, unitPrice0 : BigDecimal, amtPwr : Int, qtyScale : Int, symbol : String, gridSpace : BigDecimal, side : Side, strategy : Strategies, maxPrice : Option[BigDecimal] = None, minPrice : Option[BigDecimal] = None) : Seq[NewOrder] = {
    val newSide = if (side == Side.buy) Side.sell else Side.buy
    seed(qty0, unitPrice0, amtPwr, qtyScale, symbol, 1, gridSpace, newSide, isPulledFromOtherSide = false, strategy, maxPrice, minPrice)
  }

  def ppt(unitPrice0 : BigDecimal, qty0 : BigDecimal, amtPower : Int, rate : BigDecimal, qtyScale : Int, movement: Movement): (BigDecimal, BigDecimal) ={
    val unitPrice1 = if (movement == Movement.DOWN) unitPrice0(mc) / rate else unitPrice0 * rate
    val mtpBoost = MyUtils sqrt rate pow amtPower
    val qty1 = if (movement == Movement.DOWN) MyUtils.roundCeil(qty0 * mtpBoost, qtyScale) else MyUtils.roundFloor(qty0(mc) / mtpBoost, qtyScale)
    (unitPrice1, qty1)
  }


  def step(unitPrice0 : BigDecimal, qty0 : BigDecimal, rate : BigDecimal, qtyScale : Int, movement: Movement ): (BigDecimal, BigDecimal) ={
    val unitPrice1 = if (movement == Movement.DOWN) unitPrice0(mc) - rate else unitPrice0 + rate
    val qty1 = qty0
    (unitPrice1, qty1)
  }

  def calcMidPrice(unitPrice0 : BigDecimal, qty0 : BigDecimal, amtPower : Int, rate : BigDecimal, qtyScale : Int, side: Side, midPrice : BigDecimal) : (BigDecimal, BigDecimal) = {
    var levels = 0
    var base = (unitPrice0, qty0)
    side match {
      case Side.buy =>
        while (base._1 < midPrice) {
          levels = levels + 1
          base = ppt(base._1, base._2, amtPower, rate, qtyScale, Movement.UP)
        }
      case _ =>
        while (base._1 > midPrice) {
          levels = levels + 1
          base = ppt(base._1, base._2, amtPower, rate, qtyScale, Movement.DOWN)
        }
    }
    (base._1, base._2)
  }




  /*

  		MathContext mc = MathContext.DECIMAL64;
		BigDecimal mtp = bot.getGridSpace();
		BigDecimal unitPrice0	= isBuySeed ? last.buy.unitPrice  : last.sel.unitPrice;
		BigDecimal qty0 = isBuySeed ? last.buy.qty : last.sel.qty;

		int range = isBuySeed ? last.isBuyPulledFromSel ? 3 : 2 : last.isSelPulledFromBuy ? 3 : 2;
		List<RLOrder> res = IntStream
				.range(range, levels + range)
				.mapToObj(n -> {
					BigDecimal rate = Collections.nCopies(n, BigDecimal.ONE).stream().reduce((x, y) -> x.multiply(mtp, mc)).get();
					BigDecimal unitPrice1 = isBuySeed ? unitPrice0.divide(rate, mc) : unitPrice0.multiply(rate, mc);
					BigDecimal sqrt  = MyUtils.bigSqrt(rate);
					BigDecimal qty1 = isBuySeed ? qty0.multiply(sqrt, mc): qty0.divide(sqrt, mc);
					if (unitPrice1.compareTo(BigDecimal.ZERO) <= 0) {
						log.severe("RLOrder.buildBuySeedPct rate below zero. Check config for the pair " + bot.getPair());
					}
					BigDecimal total1 = qty1.multiply(unitPrice1, mc);
					Amount qtyAmount1		= bot.base.add(qty1);
					Amount totalAmount1  = RLOrder.amount(total1, Currency.fromString(bot.quote.currencyString()), AccountID.fromAddress(bot.quote.issuerString()));
					Direction direction1 = isBuySeed ? Direction.BUY: Direction.SELL;
					RLOrder buy = RLOrder.rateUnneeded(direction1, qtyAmount1, totalAmount1);
					return buy;
			})
	   .filter(o -> o.getQuantity().value().compareTo(BigDecimal.ZERO) > 0)
.filter(o -> o.getTotalPrice().value().compareTo(BigDecimal.ZERO) > 0)

   (XRPBTC,buys : 3
quantity:1 price:0.00009000
quantity:1 price:0.00008000
quantity:1 price:0.00007000
sells : 3
quantity:1 price:0.10000000
quantity:1 price:0.10000000
quantity:1 price:2.00000000
)
   */
}
