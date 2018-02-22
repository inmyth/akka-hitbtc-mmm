package com.mbcu.hitbtc.mmm.models.request

import com.mbcu.hitbtc.mmm.models.response.Order
import play.api.libs.json._
import play.api.libs.functional.syntax._


case class NewOrderParam (clientOrderdId : String, symbol : String, side : String, price : BigDecimal, quantity: BigDecimal)
object NewOrderParam {
  implicit val jsonFormat = Json.format[NewOrderParam]

  object Implicits {
    implicit val newOrderParamWrites = new Writes[NewOrderParam] {
      def writes(nwp: NewOrderParam): JsValue = Json.obj(
        "clientOrderdId" -> nwp.clientOrderdId,
        "symbol" -> nwp.symbol,
        "side" -> nwp.side,
        "price" -> nwp.price,
        "quantity" -> nwp.quantity
      )
    }

    implicit val walletReads: Reads[NewOrderParam] = (
      (JsPath \ "clientOrderdId").read[String] and
      (JsPath \ "symbol").read[String] and
      (JsPath \ "side").read[String] and
      (JsPath \ "price").read[BigDecimal] and
      (JsPath \ "quantity").read[BigDecimal]
      ) (NewOrderParam.apply _)
  }
}


case class NewOrder(id : String, params : NewOrderParam, method : String = "newOrder")
object NewOrder {
  implicit val jsonFormat = Json.format[NewOrder]

  def from(id : String, newOrderParam: NewOrderParam) : NewOrder = {
    new NewOrder(id, newOrderParam)
  }
}


/*
{
  "method": "newOrder",
  "params": {
    "clientOrderId": "client123456",
    "symbol": "XRPBTC",
    "side": "sell",
    "price": "0.1",
    "quantity": "1"
  },
  "id": 12345678
}
 */