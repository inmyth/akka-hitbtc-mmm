package com.mbcu.hitbtc.mmm.sequences

import java.math.MathContext

import com.mbcu.hitbtc.mmm.models.internal.Bot
import com.mbcu.hitbtc.mmm.models.response.{Order, PingPong, Side}
import com.mbcu.hitbtc.mmm.sequences.Strategy._
import com.mbcu.hitbtc.mmm.utils.MyUtils
import org.scalatest.FunSuite

import scala.math.BigDecimal.RoundingMode

class StrategyTest extends FunSuite {
  val mc : MathContext = MathContext.DECIMAL64
  val ZERO = BigDecimal("0")
  val ONE = BigDecimal("1")
  val CENT = BigDecimal("100")


  val botNoah1 = new Bot("NOAHBTC", BigDecimal("0.5"), 5, 10, BigDecimal(5000), BigDecimal(5000),
    2, None, None, -3, 10,
    true, true, true,  Strategies.ppt)

  val botNoah2 = new Bot("NOAHBTC", BigDecimal("0.0000001"), 5, 10, BigDecimal(5000), BigDecimal(5000),
    2, None, None, -3, 10,
    true, true, true, Strategies.fullfixed)

  val orderNoah1 = Order(
    "testID",
    "clientabc",
    "NOAHBTC",
    Side.sell,
    "new",
    "limit",
    "GTC",
    BigDecimal("5000"),
    BigDecimal("0.000001280"),
    BigDecimal("0"),
    "2018-02-17T21:08:01.983Z",
    "2018-02-17T21:08:01.983Z",
    None,
    None,
    "new"
  )

  implicit class ScaledBigDecimal(s: BigDecimal) {
     def ten : BigDecimal = s.setScale(10, RoundingMode.HALF_EVEN)
  }

  test("ppt seed pulledFromOtherSide") {
    val initLevel = 2

    var b = botNoah1
    var o = orderNoah1
    val pullFromBuy = Strategy.seed(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.sellGridLevels, b.gridSpace, Side.sell, PingPong.ping, true, Strategies.ppt, true, b.maxPrice, b.minPrice)

    val mtp = MyUtils.sqrt(ONE + b.gridSpace(mc) / CENT).pow(b.quantityPower)
    assert(pullFromBuy.lengthCompare(b.sellGridLevels) == 0)
    assert(pullFromBuy.head.params.price == (o.price * mtp.pow(initLevel)).ten)
    assert(pullFromBuy.head.params.quantity == BigDecimal(3000))
    assert(pullFromBuy(1).params.price == (o.price * mtp.pow(initLevel + 1)).ten)
    assert(pullFromBuy(1).params.quantity == BigDecimal(2000))
    assert(pullFromBuy(8).params.price == (o.price * mtp.pow(initLevel + 8)).ten)
    assert(pullFromBuy(8).params.quantity == BigDecimal(1000))

    val pullFromSell = Strategy.seed(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.buyGridLevels, b.gridSpace, Side.buy, PingPong.ping,true, Strategies.ppt, true, b.maxPrice, b.minPrice)
    assert(pullFromSell.lengthCompare(b.buyGridLevels) == 0)
    assert(pullFromSell.head.params.price == (o.price / mtp.pow(initLevel)).ten)
    assert(pullFromSell.head.params.quantity == BigDecimal(7000))
    assert(pullFromSell(1).params.price == (o.price / mtp.pow(initLevel + 1)).ten)
    assert(pullFromSell(1).params.quantity == BigDecimal(8000))
  }

  test("ppt counter"){
    var b = botNoah1
    var o = orderNoah1
    val mtp = MyUtils.sqrt(ONE + b.gridSpace(mc) / CENT).pow(b.quantityPower)

    val counterBuy = Strategy.counter(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.gridSpace, Side.buy, PingPong.pong, b.strategy, b.isNoQtyCutoff, b.maxPrice, b.minPrice)
    assert(counterBuy.head.params.side == Side.sell)
    assert(counterBuy.head.params.price == (o.price * mtp).ten)
    assert(counterBuy.head.params.quantity == BigDecimal(4000))

    val counterSel = Strategy.counter(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.gridSpace, Side.sell, PingPong.pong, b.strategy, b.isNoQtyCutoff, b.maxPrice, b.minPrice)
    assert(counterSel.head.params.side == Side.buy)
    assert(counterSel.head.params.price == (o.price(mc) /  mtp).ten)
    assert(counterSel.head.params.quantity == BigDecimal(6000))
  }

  test("fullFixed seed pulledFromOtherSide"){
    val initLevel = 2
    var b = botNoah2
    var o = orderNoah1
    val pullFromBuy = Strategy.seed(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.sellGridLevels, b.gridSpace, Side.sell, PingPong.ping,true, Strategies.fullfixed, true, b.maxPrice, b.minPrice)
    assert(pullFromBuy.head.params.price == o.price + initLevel * b.gridSpace)
    assert(pullFromBuy(1).params.price == o.price + (initLevel + 1) * b.gridSpace)
    assert(pullFromBuy(2).params.price == o.price + (initLevel + 2) * b.gridSpace)
    assert(pullFromBuy.head.params.quantity == o.quantity)
    assert(pullFromBuy(1).params.quantity == o.quantity)
    assert(pullFromBuy(2).params.quantity == o.quantity)

    val pullFromSell = Strategy.seed(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.buyGridLevels, b.gridSpace, Side.buy, PingPong.ping, true, Strategies.fullfixed, true, b.maxPrice, b.minPrice)
    assert(pullFromSell.head.params.price == o.price - initLevel * b.gridSpace)
    assert(pullFromSell(1).params.price == o.price - (initLevel + 1) * b.gridSpace)
    assert(pullFromSell(2).params.price == o.price - (initLevel + 2) * b.gridSpace)
    assert(pullFromSell.head.params.quantity == o.quantity)
    assert(pullFromSell(1).params.quantity == o.quantity)
    assert(pullFromSell(2).params.quantity == o.quantity)
  }

  test("fullFixed counter") {
    var b = botNoah2
    var o = orderNoah1
    val counterBuy = Strategy.counter(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.gridSpace, Side.buy, PingPong.pong, b.strategy, b.isNoQtyCutoff, b.maxPrice, b.minPrice)
    assert(counterBuy.head.params.side == Side.sell)
    assert(counterBuy.head.params.price == o.price + b.gridSpace )
    assert(counterBuy.head.params.quantity == b.sellOrderQuantity)

    val counterSell = Strategy.counter(o.quantity, o.price, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.gridSpace, Side.sell, PingPong.pong, b.strategy, b.isNoQtyCutoff, b.maxPrice, b.minPrice)
    assert(counterSell.head.params.side == Side.buy)
    assert(counterSell.head.params.price == o.price - b.gridSpace )
    assert(counterSell.head.params.quantity == b.buyOrderQuantity)
  }

//  test("full seed buy pulledFromOtherSide, negative price") {
//    val res = Strategy.seed(order.quantity, BigDecimal("0.00009"), amtPower1, NOAHScale, order.symbol, 3, BigDecimal(1), Side.buy,  isPulledFromOtherSide = true, Strategies.fullfixed)
//    assert(res.lengthCompare(0) == 0)
//  }

  test("calculate midPrice") {
    var midPrice = BigDecimal("0.000001200")
    var b = botNoah1
    var o = orderNoah1
    assert(midPrice < o.price)
    var newMid = Strategy.calcMid(o.price, o.quantity, b.quantityPower, b.gridSpace, b.counterScale, Side.sell, midPrice, b.strategy)
    var newBuySeed = Strategy.seed(newMid._2, newMid._1, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.buyGridLevels, b.gridSpace, Side.buy, PingPong.ping, false, Strategies.ppt, true, b.maxPrice, b.minPrice)
    var r = MyUtils.sqrt(ONE + b.gridSpace(mc) / CENT).pow(b.quantityPower)
    assert(newMid._1 < midPrice && newMid._1 > midPrice(mc) / r.pow(b.quantityPower))
    assert(newBuySeed.head.params.price == (newMid._1 / r.pow(1)).ten)
    assert(newBuySeed(1).params.price == (newMid._1 / r.pow(2)).ten)

    var expectedLevels = 13
    var newSellSeed = Strategy.seed(newMid._2, newMid._1, b.quantityPower, b.counterScale, b.baseScale, b.pair, expectedLevels, b.gridSpace, Side.sell, PingPong.ping, false, Strategies.ppt, true, b.maxPrice, b.minPrice)
    assert(newSellSeed.head.params.price == (newMid._1 * r.pow(1)).ten)
    assert(newSellSeed(1).params.price == (newMid._1 * r.pow(2)).ten)
    // this proves that the starting order can be reproduced with the same levels
    assert(newSellSeed.last.params.price == o.price) //5000
    assert(newSellSeed.last.params.quantity == o.quantity) // .000001280
    assert(newMid._3 == expectedLevels)


    midPrice = BigDecimal("0.000001300")
    assert(midPrice > o.price)
    newMid = Strategy.calcMid(o.price, o.quantity, b.quantityPower, b.gridSpace, b.counterScale, Side.buy, midPrice, b.strategy)
    newSellSeed = Strategy.seed(newMid._2, newMid._1, b.quantityPower, b.counterScale, b.baseScale, b.pair, b.sellGridLevels, b.gridSpace, Side.sell, PingPong.ping, false, Strategies.ppt, true, b.maxPrice, b.minPrice)
    assert(newMid._1 > midPrice && newMid._1 < midPrice * r.pow(b.quantityPower))
    assert(newSellSeed.head.params.price == (newMid._1 * r.pow(1)).ten)
    assert(newSellSeed(1).params.price == (newMid._1 * r.pow(2)).ten)

    expectedLevels = 4
    newBuySeed = Strategy.seed(newMid._2, newMid._1, b.quantityPower, b.counterScale, b.baseScale, b.pair, expectedLevels, b.gridSpace, Side.buy, PingPong.ping, false, Strategies.ppt, true, b.maxPrice, b.minPrice)
    r = MyUtils.sqrt(ONE + b.gridSpace(mc) / CENT).pow(b.quantityPower)
    assert(newBuySeed.head.params.price == (newMid._1 / r.pow(1)).ten)
    assert(newBuySeed(1).params.price == (newMid._1 / r.pow(2)).ten)
    assert(newMid._3 == expectedLevels)

  }


}
