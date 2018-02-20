package com.mbcu.hitbtc.mmm.models.request

import play.api.libs.json.Json

case class NewOrderParam (clientOrderdId : String, symbol : String, side : String, price : String, quantity: String)
object NewOrderParam {
  implicit val jsonFormat = Json.format[NewOrderParam]

  def from(clientOrderdId : String, symbol : String, side : String, price : BigDecimal, quantity: BigDecimal) : NewOrderParam = {
    new NewOrderParam(clientOrderdId, symbol, side, price.toString(), quantity.toString())
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