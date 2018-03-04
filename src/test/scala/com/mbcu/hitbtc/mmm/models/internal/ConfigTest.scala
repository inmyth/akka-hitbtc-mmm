package com.mbcu.hitbtc.mmm.models.internal

import com.mbcu.hitbtc.mmm.models.response.Order
import org.scalatest.FunSuite
import play.api.libs.json.{JsValue, Json}

class ConfigTest extends FunSuite{

  test("max price, min price missing") {
    val str = """        {
      |            "pair": "XRPBTC",
      |            "startMiddlePrice": "10",
      |            "gridSpace": "0.5",
      |            "buyGridLevels": 2,
      |            "sellGridLevels": 2,
      |            "buyOrderQuantity": "1",
      |            "sellOrderQuantity": "1",
      |            "qtyScale" : 0,
      |            "strategy" : "ppt"
      |        }""".stripMargin
    val obj = Json.parse(str).as[Bot]
    assert(obj.maxPrice.isEmpty)
    assert(obj.minPrice.isEmpty)
  }

  test("max price, min price exist") {
    val str = """        {
                |            "pair": "XRPBTC",
                |            "startMiddlePrice": "10",
                |            "gridSpace": "0.5",
                |            "buyGridLevels": 2,
                |            "sellGridLevels": 2,
                |            "buyOrderQuantity": "1",
                |            "sellOrderQuantity": "1",
                |            "maxPrice" : "15",
                |            "minPrice" : "5",
                |            "qtyScale" : 0,
                |            "strategy" : "ppt"
                |        }""".stripMargin
    val obj = Json.parse(str).as[Bot]
    assert(obj.maxPrice.contains(BigDecimal("15")))
    assert(obj.minPrice.contains(BigDecimal("5")))
  }
}