package com.mbcu.hitbtc.mmm.models.internal

import com.mbcu.hitbtc.mmm.models.response.Order
import org.scalatest.FunSuite
import play.api.libs.json.{JsValue, Json}

class ConfigTest extends FunSuite{

  test("max price, min price missing") {
  val str = """{
                |                "pair": "NOAHBTC",
                |                "gridSpace": "0.5",
                |                "buyGridLevels": 2,
                |                "sellGridLevels": 2,
                |                "buyOrderQuantity": "1",
                |                "sellOrderQuantity": "1",
                |                "quantityPower" : 1,
                |                "counterScale" : -3,
                |                "baseScale" : 9,
                |                "isStrictLevels" : true,
                |                "isNoQtyCutoff" : true,
                |                "strategy" : "ppt"
                |                }""".stripMargin
    val obj = Json.parse(str).as[Bot]
    assert(obj.maxPrice.isEmpty)
    assert(obj.minPrice.isEmpty)
  }

  test("max price, min price exist") {
    val str = """        {
                |                "pair": "NOAHBTC",
                |                "gridSpace": "0.5",
                |                "buyGridLevels": 2,
                |                "sellGridLevels": 2,
                |                "buyOrderQuantity": "1",
                |                "sellOrderQuantity": "1",
                |                "quantityPower" : 1,
                |                "counterScale" : -3,
                |                "baseScale" : 9,
                |                "isStrictLevels" : true,
                |                "isNoQtyCutoff" : true,
                |                "strategy" : "ppt",
                |                "minPrice" : "0.00000001",
                |                "maxPrice" : "0.00000003"
                |        }""".stripMargin
    val obj = Json.parse(str).as[Bot]
    assert(obj.maxPrice.contains(BigDecimal(".00000003")))
    assert(obj.minPrice.contains(BigDecimal(".00000001")))
  }
}
