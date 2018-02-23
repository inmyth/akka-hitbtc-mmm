package com.mbcu.hitbtc.mmm.models.response

import com.mbcu.hitbtc.mmm.models.request.Login
import org.scalatest.FunSuite
import play.api.libs.json.{JsArray, JsValue, Json}

class OrderTest extends FunSuite{

  test("New Order") {
    val str = """{
    |   "jsonrpc":"2.0",
    |   "method":"report",
    |   "params":{
    |      "id":"18466003336",
    |      "clientOrderId":"60f4087535c34106bf81d6d9a0ecb213",
    |      "symbol":"XRPBTC",
    |      "side":"buy",
    |      "status":"new",
    |      "type":"limit",
    |      "timeInForce":"GTC",
    |      "quantity":"100",
    |      "price":"0.00009179",
    |      "cumQuantity":"0",
    |      "createdAt":"2018-02-22T10:29:48.730Z",
    |      "updatedAt":"2018-02-22T10:29:48.730Z",
    |      "reportType":"new"
    |   }
    |}""".stripMargin
    val obj2Json: JsValue = Json.parse(str)
    assert((obj2Json \ "method").as[String] == "report")
    val param = (obj2Json \ "params").as[Order]
    assert(param.id == "18466003336")
    assert(param.reportType == "new")
  }

  test("Order filled") {
    val str ="""{
       |   "jsonrpc":"2.0",
       |   "method":"report",
       |   "params":{
       |      "id":"18466003336",
       |      "clientOrderId":"60f4087535c34106bf81d6d9a0ecb213",
       |      "symbol":"XRPBTC",
       |      "side":"buy",
       |      "status":"filled",
       |      "type":"limit",
       |      "timeInForce":"GTC",
       |      "quantity":"100",
       |      "price":"0.00009179",
       |      "cumQuantity":"100",
       |      "createdAt":"2018-02-22T10:29:48.730Z",
       |      "updatedAt":"2018-02-22T10:33:58.136Z",
       |      "reportType":"trade",
       |      "tradeQuantity":"25",
       |      "tradePrice":"0.00009179",
       |      "tradeId":204766318,
       |      "tradeFee":"-0.000000229"
       |   }
       |}""".stripMargin

    val obj2Json: JsValue = Json.parse(str)
    assert((obj2Json \ "method").as[String] == "report")
    val param = (obj2Json \ "params").as[Order]
    assert(param.id == "18466003336")
    assert(param.reportType == "trade")
    assert(param.tradeId == Some(204766318))
    assert(param.tradeFee == Some(BigDecimal("-0.000000229")))
    assert(param.status == "filled")
  }

  test("Order partially filled") {
    val str = """{
      |   "jsonrpc":"2.0",
      |   "method":"report",
      |   "params":{
      |      "id":"18466003336",
      |      "clientOrderId":"60f4087535c34106bf81d6d9a0ecb213",
      |      "symbol":"XRPBTC",
      |      "side":"buy",
      |      "status":"partiallyFilled",
      |      "type":"limit",
      |      "timeInForce":"GTC",
      |      "quantity":"100",
      |      "price":"0.00009179",
      |      "cumQuantity":"9",
      |      "createdAt":"2018-02-22T10:29:48.730Z",
      |      "updatedAt":"2018-02-22T10:32:12.010Z",
      |      "reportType":"trade",
      |      "tradeQuantity":"9",
      |      "tradePrice":"0.00009179",
      |      "tradeId":204764649,
      |      "tradeFee":"-0.000000082"
      |   }
      |}""".stripMargin
    val obj2Json: JsValue = Json.parse(str)
    assert((obj2Json \ "method").as[String] == "report")
    val param = (obj2Json \ "params").as[Order]
    assert(param.id == "18466003336")
    assert(param.reportType == "trade")
    assert(param.cumQuantity == BigDecimal("9"))
    assert(param.tradePrice == Some(BigDecimal("0.00009179")))
    assert(param.tradeQuantity == Some(BigDecimal("9")))
  }

  test("ActiveOrders Test ") {
    val str = """{
      |   "jsonrpc":"2.0",
      |   "method":"activeOrders",
      |   "params":[
      |      {
      |         "id":"17626743960",
      |         "clientOrderId":"client123456",
      |         "symbol":"XRPBTC",
      |         "side":"sell",
      |         "status":"new",
      |         "type":"limit",
      |         "timeInForce":"GTC",
      |         "quantity":"1",
      |         "price":"0.10000000",
      |         "cumQuantity":"0",
      |         "createdAt":"2018-02-17T03:14:42.743Z",
      |         "updatedAt":"2018-02-17T03:14:42.743Z",
      |         "reportType":"status"
      |      },
      |      {
      |         "id":"17623980170",
      |         "clientOrderId":"3f494796175b45b0b0ebae048543460b",
      |         "symbol":"XRPBTC",
      |         "side":"sell",
      |         "status":"new",
      |         "type":"limit",
      |         "timeInForce":"GTC",
      |         "quantity":"1",
      |         "price":"0.10000000",
      |         "cumQuantity":"0",
      |         "createdAt":"2018-02-17T02:50:25.012Z",
      |         "updatedAt":"2018-02-17T02:50:25.012Z",
      |         "reportType":"status"
      |      }
      |   ]
      |}""".stripMargin

    val obj2Json: JsValue = Json.parse(str)
    assert((obj2Json \ "method").as[String] == "activeOrders")
//    val param = (obj2Json \ "params").as[Order]

  }


}
