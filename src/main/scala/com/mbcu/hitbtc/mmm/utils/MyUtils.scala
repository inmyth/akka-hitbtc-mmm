package com.mbcu.hitbtc.mmm.utils

import java.math.MathContext

object MyUtils {

  def sqrt(a: BigDecimal, scale: Int = 16): BigDecimal = {
    var x = BigDecimal( Math.sqrt(a.doubleValue()), MathContext.DECIMAL64 )

    if (scale < 17) {
      return x
    }

    val b2 = BigDecimal(2)
    var tempScale = 16
    while(tempScale < scale){
      x = x - (x * x - a) / (2 * x);
//      x = x - (x * x.subtract(a).divide(x.multiply(b2), scale, BigDecimal.ROUND_HALF_EVEN))
      tempScale *= 2
    }
    x
  }

}
