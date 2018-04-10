package com.mbcu.hitbtc.mmm.utils

import java.math.MathContext
import java.util.TimeZone

import com.mbcu.hitbtc.mmm.models.response.Order
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

  test ("tuple pattern match") {
    val a = (5, 6)
    val b = (0, 6)

    def tuplePM(in : (Int, Int)) : Unit = {
      in match {
        case _ if in._1 > 0 && in._2 > 0 => assert(in._1 > 0)
        case _ if in._1 == 0 && in._2 > 0 => assert(in._1 === 0)
      }
    }

    tuplePM(a)
    tuplePM(b)


  }

  test("test add tuple to seq")  {
    var res : scala.collection.immutable.Seq[(Int, BigDecimal, BigDecimal, Boolean)] = scala.collection.immutable.Seq.empty
    res = (5, BigDecimal(5), BigDecimal(5), false) +: res
    res = (7, BigDecimal(7), BigDecimal(7), false) +: res
    assert(res.head._1 === 7)

  }

  test("series to reduce") {
    val a = BigDecimal(4)
    val b = BigDecimal("13.4")
    val c = a.to(b, BigDecimal(5))
      .reduce((l, r) => r)
    assert(c === BigDecimal(9))
  }

  test ("while loop ") {
    var levels = 0
    var a = BigDecimal("1")
    val m = BigDecimal("1.01")
    val b = BigDecimal("1.03")

    while (a < b) {
      levels = levels + 1
      a = a * m
    }
    assert(levels === 3)
  }



  test("test foldLeft"){
    def abc(x : Int, y : Int) : (Int, Int) = {
      (x + 2, y +2)
    }

    val a = List(1,1,1)
    val b = a.foldLeft(1,1)((p,q) =>
    {
      println(p)
      abc(p._1, p._2)

    }

    )
    println("--")
    println(b)

  }


}
