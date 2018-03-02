package com.mbcu.hitbtc.mmm.sequences

import java.math.MathContext

import com.mbcu.hitbtc.mmm.models.response.Order
import com.mbcu.hitbtc.mmm.sequences.Strategy.{ONE, Strategies, mc}
import com.mbcu.hitbtc.mmm.utils.MyUtils
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, FunSuite}

class StrategyTest extends FunSuite {
  val mc : MathContext = MathContext.DECIMAL64
  val ZERO = BigDecimal("0")
  val ONE = BigDecimal("1")
  val CENT = BigDecimal("100")

  val qty = BigDecimal("1")
  var price = BigDecimal("10")
  val gridSpace = BigDecimal("1")
  val mtp: BigDecimal = ONE + gridSpace(mc) / CENT
  val XRPscale = 3

  val order = Order(
    "testID",
    "clientabc",
    "XRPBTC",
    "sell",
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
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "sell",  isPulledFromOtherSide = true, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price * mtp * mtp)
    assert(res.head.params.quantity == MyUtils.roundFloor(qty(mc) / mtp, XRPscale))
    assert(res(1).params.price / res.head.params.price == mtp)
    //    assert(res(1).params.quantity(mc) / res(0).params.quantity == ONE(mc) / MyUtils.sqrt(mtp))
  }


  test("ppt seed sell") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "sell",  isPulledFromOtherSide = false, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price * mtp)
    assert(res.head.params.quantity == MyUtils.roundFloor(qty(mc) / MyUtils.sqrt(mtp), XRPscale))
    assert(res(1).params.price / res.head.params.price == mtp)
    //    assert((res(1).params.quantity(mc) / res.head.params.quantity) == ONE(mc) / MyUtils.sqrt(mtp))
  }

  test("ppt buy sell pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "buy",  isPulledFromOtherSide = true, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price(mc) /  mtp / mtp)
    assert(res.head.params.quantity == qty(mc) * mtp )
    assert(res.head.params.price / res(1).params.price == mtp)
    assert(res.head.params.price / res(2).params.price == mtp * mtp)
  }


  test("ppt seed buy") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "buy", isPulledFromOtherSide = false, Strategies.ppt)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price(mc) /  mtp )
    assert(res.head.params.quantity == MyUtils.roundCeil(qty(mc) * MyUtils.sqrt(mtp), XRPscale))
    assert(res.head.params.price / res(1).params.price == mtp)
    assert(res.head.params.price / res(2).params.price == mtp * mtp)
  }

  test("ppt counter from buy"){
    val res = Strategy.counter(order.quantity, order.price, XRPscale, order.symbol, BigDecimal("1"), "buy", Strategies.ppt)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == "sell")
    assert(res.head.params.price == price(mc) *  mtp )
    assert(res.head.params.quantity == MyUtils.roundFloor(qty(mc) / MyUtils.sqrt(mtp), XRPscale))
  }

  test("ppt counter from sell"){
    val res = Strategy.counter(order.quantity, order.price, XRPscale, order.symbol, BigDecimal("1"), "sell", Strategies.ppt)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == "buy")
    assert(res.head.params.price == price(mc) /  mtp )
    assert(res.head.params.quantity == MyUtils.roundCeil(qty(mc) * MyUtils.sqrt(mtp), XRPscale))
  }

  test("full seed sell pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "sell",  isPulledFromOtherSide = true, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price + 2 * gridSpace)
    assert(res(1).params.price == price + 3 * gridSpace)
    assert(res(2).params.price == price + 4 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }


  test("full seed sell") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "sell",  isPulledFromOtherSide = false, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price + 1 * gridSpace)
    assert(res(1).params.price == price + 2 * gridSpace)
    assert(res(2).params.price == price + 3 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }



  test("full seed buy pulledFromOtherSide") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "buy",  isPulledFromOtherSide = true, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price - 2 * gridSpace)
    assert(res(1).params.price == price - 3 * gridSpace)
    assert(res(2).params.price == price - 4 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }


  test("full seed buy") {
    val res = Strategy.seed(order.quantity, order.price, XRPscale, order.symbol, 3, BigDecimal(1), "buy",  isPulledFromOtherSide = false, Strategies.fullfixed)
    assert(res.lengthCompare(3) == 0)
    assert(res.head.params.price == price - 1 * gridSpace)
    assert(res(1).params.price == price - 2 * gridSpace)
    assert(res(2).params.price == price - 3 * gridSpace)
    assert(res.head.params.quantity == qty)
    assert(res(1).params.quantity == qty)
    assert(res(2).params.quantity == qty)
  }

  test("full counter from buy"){
    val res = Strategy.counter(order.quantity, order.price, XRPscale, order.symbol, BigDecimal("1"), "buy", Strategies.fullfixed)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == "sell")
    assert(res.head.params.price == price +  gridSpace )
    assert(res.head.params.quantity == qty)
  }

  test("full counter from sell"){
    val res = Strategy.counter(order.quantity, order.price, XRPscale, order.symbol, BigDecimal("1"), "sell", Strategies.fullfixed)
    assert(res.lengthCompare(1) == 0)
    assert(res.head.params.side == "buy")
    assert(res.head.params.price == price(mc) - gridSpace )
    assert(res.head.params.quantity == qty)
  }


  test("full seed buy pulledFromOtherSide, negative price") {
    val res = Strategy.seed(order.quantity, BigDecimal("0.00009"), XRPscale, order.symbol, 3, BigDecimal(1), "buy",  isPulledFromOtherSide = true, Strategies.fullfixed)
    assert(res.lengthCompare(0) == 0)
  }

}
