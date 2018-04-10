package com.mbcu.hitbtc.mmm.sequences

import java.math.MathContext

import com.mbcu.hitbtc.mmm.models.response.{Order, Side}
import com.mbcu.hitbtc.mmm.sequences.Strategy.{ONE, Strategies, mc}
import com.mbcu.hitbtc.mmm.utils.MyUtils
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.math.BigDecimal.RoundingMode

class StrategyTest extends FunSuite {
  val mc : MathContext = MathContext.DECIMAL64
  val ZERO = BigDecimal("0")
  val ONE = BigDecimal("1")
  val CENT = BigDecimal("100")

  val qty = BigDecimal("1")
  val price = BigDecimal("10")
  val gridSpace = BigDecimal("1")
  val mtp: BigDecimal = ONE + gridSpace(mc) / CENT
  val XRPscale = 3
  val amtPower1 = 1
  val amtPower2 = 2

  val order = Order(
    "testID",
    "clientabc",
    "XRPBTC",
    Side.sell,
    "new",
    "limit",
    "GTC",
    qty,
    price,
    BigDecimal("0"),
    "2018-02-17T21:08:01.983Z",
    "2018-02-17T21:08:01.983Z",
    None,
    None,
    "new"
  )



  test("ppt seed sell pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.sell,  isPulledFromOtherSide = true, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price * mtp * mtp)
    assert(res.head.params.quantity == MyUtils.roundFloor(qty(mc) / mtp, XRPscale))
    assert(res(1).params.price / res.head.params.price == mtp)
    //    assert(res(1).params.quantity(mc) / res(0).params.quantity == ONE(mc) / MyUtils.sqrt(mtp))
  }


  test("ppt seed sell") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.sell,  isPulledFromOtherSide = false, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price * mtp)
    assert(res.head.params.quantity == MyUtils.roundFloor(qty(mc) / MyUtils.sqrt(mtp), XRPscale))
    assert(res(1).params.price / res.head.params.price == mtp)
    println(res)
    //    assert((res(1).params.quantity(mc) / res.head.params.quantity) == ONE(mc) / MyUtils.sqrt(mtp))
  }

  test("ppt buy sell pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.buy,  isPulledFromOtherSide = true, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price(mc) /  mtp / mtp)
    assert(res.head.params.quantity == qty(mc) * mtp )
    assert(res.head.params.price / res(1).params.price == mtp)
    assert(res.head.params.price / res(2).params.price == mtp * mtp)
  }


  test("ppt seed buy") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.buy, isPulledFromOtherSide = false, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price(mc) /  mtp )
    assert(res.head.params.quantity == MyUtils.roundCeil(qty(mc) * MyUtils.sqrt(mtp), XRPscale))
    assert(res.head.params.price / res(1).params.price == mtp)
    assert(res.head.params.price / res(2).params.price == mtp * mtp)
  }

  test("ppt counter from buy"){
    val res = Strategy.counter(order.quantity, order.price, amtPower1, XRPscale, order.symbol, BigDecimal("1"), Side.buy, Strategies.ppt)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == Side.sell)
    assert(res.head.params.price == price(mc) *  mtp )
    assert(res.head.params.quantity == MyUtils.roundFloor(qty(mc) / MyUtils.sqrt(mtp), XRPscale))
  }

  test("ppt counter from sell"){
    val res = Strategy.counter(order.quantity, order.price, amtPower1, XRPscale, order.symbol, BigDecimal("1"), Side.sell, Strategies.ppt)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == Side.buy)
    assert(res.head.params.price == price(mc) /  mtp )
    assert(res.head.params.quantity == MyUtils.roundCeil(qty(mc) * MyUtils.sqrt(mtp), XRPscale))
  }

  test("full seed sell pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.sell,  isPulledFromOtherSide = true, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price + 2 * gridSpace)
    assert(res(1).params.price == price + 3 * gridSpace)
    assert(res(2).params.price == price + 4 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }


  test("full seed sell") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.sell,  isPulledFromOtherSide = false, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price + 1 * gridSpace)
    assert(res(1).params.price == price + 2 * gridSpace)
    assert(res(2).params.price == price + 3 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }



  test("full seed buy pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.buy,  isPulledFromOtherSide = true, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price - 2 * gridSpace)
    assert(res(1).params.price == price - 3 * gridSpace)
    assert(res(2).params.price == price - 4 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }


  test("full seed buy") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.buy,  isPulledFromOtherSide = false, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price - 1 * gridSpace)
    assert(res(1).params.price == price - 2 * gridSpace)
    assert(res(2).params.price == price - 3 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }

  test("full counter from buy"){
    val res = Strategy.counter(order.quantity, order.price, amtPower1, XRPscale, order.symbol, BigDecimal("1"), Side.buy, Strategies.fullfixed)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == Side.sell)
    assert(res.head.params.price == price +  gridSpace )
    assert(res.head.params.quantity == qty)
  }

  test("full counter from sell"){
    val res = Strategy.counter(order.quantity, order.price, amtPower1, XRPscale, order.symbol, BigDecimal("1"), Side.sell, Strategies.fullfixed)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == Side.buy)
    assert(res.head.params.price == price(mc) - gridSpace )
    assert(res.head.params.quantity == qty)
  }


  test("full seed buy pulledFromOtherSide, negative price") {
    val res = Strategy.seed(order.quantity, BigDecimal("0.00009"), amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.buy,  isPulledFromOtherSide = true, Strategies.fullfixed)
    assert(res.lengthCompare(0) == 0)
  }

  test("full seed buy with minPrice, 10 levels") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 10, BigDecimal(1), Side.buy,  isPulledFromOtherSide = false, Strategies.fullfixed, None, Some(BigDecimal(5)))
    assert(res.lengthCompare(5) == 0)
    assert(res.head.params.price == price - 1 * gridSpace)
    assert(res(1).params.price == price - 2 * gridSpace)
    assert(res(2).params.price == price - 3 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }

  test("full seed sell with maxPrice, 10 levels") {
    val res = Strategy.seed(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 10, BigDecimal(1), Side.sell,  isPulledFromOtherSide = false, Strategies.fullfixed, Some(BigDecimal("15")), None)
    assert(res.lengthCompare(5) == 0)
    assert(res.head.params.price == price + 1 * gridSpace)
    assert(res(1).params.price == price + 2 * gridSpace)
    assert(res(2).params.price == price + 3 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }

  test("full counter from buy with maxPrice"){
    val res = Strategy.counter(order.quantity, order.price, amtPower1, XRPscale, order.symbol, BigDecimal("1"), Side.buy, Strategies.fullfixed, Some(BigDecimal("10.7")), None)
    assert(res.lengthCompare(0) == 0)
  }

  test("full counter from sell with minPrice"){
    val res = Strategy.counter(order.quantity, order.price, amtPower1, XRPscale, order.symbol, BigDecimal("1"), Side.sell, Strategies.fullfixed,  None,  Some(BigDecimal("9.2")))
    assert(res.lengthCompare(0) == 0)
  }


//  test("reconstruct orders from any buy order far from spread") {
//    var unitPrice0 = BigDecimal("1")
//    val qty0  = BigDecimal("800")
//    val midPrice = BigDecimal("1.02")
//    val rate = BigDecimal("1.01")
//    val buyGridLevels = 3
//    val res1 = Strategy.calcMid(unitPrice0, qty0, amtPower1, rate, XRPscale, Side.buy, midPrice, Strategies.ppt)
//    val res2 = Strategy.seed(res1._2, res1._1, amtPower1, 2, order.symbol, buyGridLevels, BigDecimal(1), Side.buy,  isPulledFromOtherSide = false, Strategies.ppt)
//
//    assert(res1._1 === BigDecimal("1.0201"))
//    assert(res1._2 === BigDecimal("792.078"))
//
////    assert(res2.head.params.price === BigDecimal("1.01"))
////    assert(res2.head.params.quantity === BigDecimal("796.1"))
////    assert(res2(1).params.price === BigDecimal("1"))
////    assert(res2(1).params.quantity === BigDecimal("800"))
////    assert(res2(2).params.price === BigDecimal("0.9900990099009901"))
////    assert(res2(2).params.quantity === BigDecimal("804.0"))
//
//    assert(res2(1).params.price === unitPrice0)
//    assert(res2(1).params.quantity === qty0)
//  }
//
//  test("reconstruct order zero from any buy order close to spread") {
//    var unitPrice0 = BigDecimal("1")
//    val qty0  = BigDecimal("800")
//    val midPrice = BigDecimal("1.005")
//    val rate = BigDecimal("1.01")
//    val buyGridLevels = 2
//
//    val res1 = Strategy.calcMid(unitPrice0, qty0, amtPower1, rate, XRPscale, Side.buy, midPrice, Strategies.ppt)
//    val res2 = Strategy.seed(res1._2, res1._1, amtPower1, XRPscale, order.symbol, buyGridLevels, BigDecimal(1), Side.buy,  isPulledFromOtherSide = false, Strategies.ppt)
//
//    assert(res2.head.params.price === unitPrice0)
//    assert(res2.head.params.quantity === qty0)
//
////    assert(res1._1 === BigDecimal("1.01"))
////    assert(res1._2 === BigDecimal("796.029"))
////    assert(res2.head.params.price === BigDecimal("1"))
////    assert(res2.head.params.quantity === BigDecimal("800"))
////    assert(res2(1).params.price === BigDecimal(".9900990099009901"))
////    assert(res2(1).params.quantity === BigDecimal("804"))
//
//
//  }
//
//  test("reconstruct order zero from sell order far from spread") {
//    var unitPrice0 = BigDecimal("1")
//    val qty0  = BigDecimal("800")
//    val midPrice = BigDecimal("0.98")
//    val rate = BigDecimal("1.01")
//    val selGridLevels = 3
//    val res1 = Strategy.calcMid(unitPrice0, qty0, amtPower1, rate, XRPscale, Side.sell, midPrice, Strategies.ppt)
//    val res2 = Strategy.seed(res1._2, res1._1, amtPower2, XRPscale, order.symbol, selGridLevels, BigDecimal(1), Side.sell,  isPulledFromOtherSide = false, Strategies.ppt)
//    assert(res1._1 === BigDecimal("0.9705901479276445"))
//    assert(res1._2 === BigDecimal("824.241"))
//
////    assert(res2.head.params.price === BigDecimal("0.9802960494069209"))
////    assert(res2.head.params.quantity === BigDecimal("816.080"))
////    assert(res2(1).params.price === BigDecimal("0.9900990099009902"))
////    assert(res2(1).params.quantity === BigDecimal("808.000"))
////    assert(res2(2).params.price === BigDecimal("1"))
////    assert(res2(2).params.quantity === BigDecimal("800.000"))
//
//    assert(res2(2).params.price === unitPrice0)
//    assert(res2(2).params.quantity === qty0)
//  }
//
//  test("reconstruct order zero from sell order close to spread") {
//    var unitPrice0 = BigDecimal("1")
//    val qty0  = BigDecimal("800")
//    val midPrice = BigDecimal("0.991")
//    val rate = BigDecimal("1.01")
//    val selGridLevels = 3
//    val res1 = Strategy.calcMid(unitPrice0, qty0, amtPower1, rate, XRPscale, Side.sell, midPrice, Strategies.ppt)
//    val res2 = Strategy.seed(res1._2, res1._1, amtPower2, XRPscale, order.symbol, selGridLevels, BigDecimal(1), Side.sell,  isPulledFromOtherSide = false, Strategies.ppt)
//    assert(res1._1 === BigDecimal("0.9900990099009901"))
//    assert(res1._2 === BigDecimal("808.0"))
//
//
//    assert(res2.head.params.price === unitPrice0)
//    assert(res2.head.params.quantity === qty0)
////    assert(res2.head.params.price === BigDecimal("1"))
////    assert(res2.head.params.quantity === BigDecimal("800"))
////    assert(res2(1).params.price === BigDecimal("1.010000000000000"))
////    assert(res2(1).params.quantity === BigDecimal("792.079"))
////    assert(res2(2).params.price === BigDecimal("1.020100000000000"))
////    assert(res2(2).params.quantity === BigDecimal("784.236"))
//  }

  test("seed2 test"){
    val res = Strategy.seed2(order.quantity, order.price, amtPower1, XRPscale, order.symbol, 3, BigDecimal(1), Side.sell,  isPulledFromOtherSide = true, Strategies.ppt)

    val a = List(1,1,1)
    val b = a.foldLeft(0)(_ + _)

    
//    // This is the only valid order
//    0 + 1 = 1
//    1 + 3 = 4
//    4 + 5 = 9
//    9 + 7 = 16
//    16 + 9 = 25 // done

  }



}
