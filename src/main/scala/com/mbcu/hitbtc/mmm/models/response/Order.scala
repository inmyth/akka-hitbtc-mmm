package com.mbcu.hitbtc.mmm.models.response

import play.api.libs.json._
import play.api.libs.functional.syntax._

object Order {
  implicit val jsonFormat = Json.format[Order]

  object Implicits {
    implicit val orderWrites = new Writes[Order] {
      def writes(order: Order): JsValue = Json.obj(
        "id" -> order.id,
        "clientOrderId" -> order.clientOrderId,
        "symbol" -> order.symbol,
        "side" -> order.side,
        "status" -> order.status,
        "type" -> order.`type`,
        "timeInForce" -> order.timeInForce,
        "quantity" -> order.quantity,
        "price" -> order.price,
        "cumQuantity" -> order.cumQuantity,
        "createdAt" -> order.createdAt,
        "updatedAt" -> order.updatedAt,
        "reportType" -> order.reportType,

      )
    }

    implicit val walletReads: Reads[Order] = (
        (JsPath \ "id").read[String] and
        (JsPath \ "clientOrderId").read[String] and
        (JsPath \ "symbol").read[String] and
        (JsPath \ "side").read[String] and
        (JsPath \ "status").read[String] and
        (JsPath \ "type").read[String] and
        (JsPath \ "timeInForce").read[String] and
        (JsPath \ "quantity").read[BigDecimal] and
        (JsPath \ "price").read[BigDecimal] and
        (JsPath \ "cumQuantity").read[String] and
        (JsPath \ "createdAt").read[String] and
        (JsPath \ "updatedAt").read[String] and
        (JsPath \ "reportType").read[String]
      ) (Order.apply _)
  }
}

case class Order (
  id : String,
  clientOrderId : String,
  symbol : String,
  side : String,
  status : String,
  `type` : String,
  timeInForce : String,
  quantity : BigDecimal,
  price : BigDecimal,
  cumQuantity : String,
  createdAt : String,
  updatedAt : String,
  reportType : String
)






/*
      "id": "17626743960",
      "clientOrderId": "client123456",
      "symbol": "XRPBTC",
      "side": "sell",
      "status": "new",
      "type": "limit",
      "timeInForce": "GTC",
      "quantity": "1",
      "price": "0.10000000",
      "cumQuantity": "0",
      "createdAt": "2018-02-17T03:14:42.743Z",
      "updatedAt": "2018-02-17T03:14:42.743Z",
      "reportType": "status"
 */