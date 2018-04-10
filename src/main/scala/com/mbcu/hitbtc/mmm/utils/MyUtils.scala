package com.mbcu.hitbtc.mmm.utils

import java.math.MathContext
import java.text.SimpleDateFormat
import java.time.{LocalDateTime, ZoneId}
import java.util.{Date, SimpleTimeZone, TimeZone}

import com.mbcu.hitbtc.mmm.models.response.Side
import com.mbcu.hitbtc.mmm.models.response.Side.Side

import scala.math.BigDecimal.RoundingMode


object MyUtils {

  def sqrt(a: BigDecimal, scale: Int = 16): BigDecimal = {
    val mc = MathContext.DECIMAL64
    var x = BigDecimal( Math.sqrt(a.doubleValue()), mc )

    if (scale < 17) {
      return x
    }

    var tempScale = 16
    while(tempScale < scale){
      x = x - (x * x - a)(mc) / (2 * x)
      tempScale *= 2
    }
    x
  }


  def date(tz : TimeZone =  TimeZone.getTimeZone("Asia/Tokyo")) : String = {
    val ldt = LocalDateTime.now()
    val zone = ZoneId.of("+09:00")
    ldt.atZone(zone).toString
  }

  def sha256Hash(text: String) : String = String.format("%064x", new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"))))

//  def clientOrderId(pair : String) : String = {
//    pair + dateForId()
//  }

  def clientOrderId(pair : String, side : Side): String ={
    val random = scala.util.Random.alphanumeric.take(15).mkString
    val hash = sha256Hash(random + System.currentTimeMillis()).substring(0, 18)
    s"$pair.$side.$hash"
  }

  def symbolFromId(id :String) : Option[String] = {
    id.split("[.]").lift(0)
  }

  def sideFromId(id : String) : Option[Side] = {
    val str = id.split("[.]").lift(1)
    str match {
      case Some(s) => Side.withNameOpt(s)
      case _ => None
    }
  }

  def roundCeil(a : BigDecimal, scale : Int): BigDecimal ={
    a.setScale(scale, RoundingMode.CEILING)
  }

  def roundFloor(a : BigDecimal, scale : Int): BigDecimal ={
    a.setScale(scale, RoundingMode.FLOOR)
  }

  def roundHalfDown(a : BigDecimal, scale : Int) : BigDecimal = {
    a.setScale(scale, RoundingMode.HALF_DOWN)
  }


}
