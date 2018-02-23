package com.mbcu.hitbtc.mmm.sequences

import java.math.MathContext

import com.mbcu.hitbtc.mmm.models.response.Order
import com.mbcu.hitbtc.mmm.sequences.Strategy.{ONE, mc}
import com.mbcu.hitbtc.mmm.utils.MyUtils
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, FunSuite}

class StrategyTest extends FunSuite {
  val mc : MathContext = MathContext.DECIMAL64
  val ZERO = BigDecimal("0")
  val ONE = BigDecimal("1")
  val CENT = BigDecimal("100")

  val qty = BigDecimal("1")
  val price = BigDecimal("0.00009")
  val gridSpace = BigDecimal("1")
  val mtp = ONE + gridSpace(mc) / CENT

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

  test("seed sell pulledFromOtherSide") {
    val res = Strategy.pptSeed(order, 3, BigDecimal(1), "sell", true)
    assert(res.size == 3)
    assert(res(0).params.price == price * mtp * mtp)
    assert(res(0).params.quantity == qty(mc) / mtp )
    assert(res(1).params.price / res(0).params.price == mtp)
//    assert(res(1).params.quantity(mc) / res(0).params.quantity == ONE(mc) / MyUtils.sqrt(mtp))
  }


  test("seed sell") {
    val res = Strategy.pptSeed(order, 3, BigDecimal(1), "sell", false)
    assert(res.size == 3)
    assert(res(0).params.price == price * mtp)
    assert(res(0).params.quantity == qty(mc) / MyUtils.sqrt(mtp))
    assert(res(1).params.price / res(0).params.price == mtp)
//    assert(res(1).params.quantity(mc) / res(0).params.quantity == ONE(mc) / MyUtils.sqrt(mtp))
  }

  test("buy sell pulledFromOtherSide") {
    val res = Strategy.pptSeed(order, 3, BigDecimal(1), "buy", true)
    assert(res.size == 3)
    assert(res(0).params.price == price(mc) /  mtp / mtp)
    assert(res(0).params.quantity == qty(mc) * mtp )
    assert(res(0).params.price / res(1).params.price == mtp)
    assert(res(0).params.price / res(2).params.price == mtp * mtp)
  }


  test("seed buy") {
    val res = Strategy.pptSeed(order, 3, BigDecimal(1), "buy", false)
    assert(res.size == 3)
    assert(res(0).params.price == price(mc) /  mtp )
    assert(res(0).params.quantity == qty(mc) * MyUtils.sqrt(mtp))
    assert(res(0).params.price / res(1).params.price == mtp)
    assert(res(0).params.price / res(2).params.price == mtp * mtp)
  }


}
