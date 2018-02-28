package com.mbcu.hitbtc.mmm.utils

import java.math.MathContext
import java.util.TimeZone

import org.scalatest.FunSuite

import scala.collection.immutable.Range

class MyUtilsTest extends FunSuite {
//  test("date id Tokyo") {
//    val clientOrderId = MyUtils.clientOrderId("BTCXRP")
//    println(clientOrderId)
//    assert(clientOrderId.contains("T"))
//  }

  test ("rounding XRP") {
    val scale = 0
    val t1 = BigDecimal("2.0345")
    val t2 = BigDecimal("3.634")
    assert(BigDecimal("2") == MyUtils.roundHalfDown(t1, scale))
    assert(BigDecimal("4") == MyUtils.roundHalfDown(t2, scale))
    assert(BigDecimal("3") == MyUtils.roundCeil(t1, scale))
    assert(BigDecimal("3") == MyUtils.roundFloor(t2, scale))
  }

  test ("rounding DOGE") {
    val scale = -3
    val t1 = BigDecimal("40345")

    val t2 = BigDecimal("49530")
    assert(BigDecimal("49000") == MyUtils.roundFloor(t2, scale))
    assert(BigDecimal("41000") == MyUtils.roundCeil(t1, scale))
  }

  test ("rounding ETH") {
    val scale = 3
    val t1 = BigDecimal("5.12340021")
    assert(BigDecimal("5.123") == MyUtils.roundFloor(t1, scale))
  }
}
