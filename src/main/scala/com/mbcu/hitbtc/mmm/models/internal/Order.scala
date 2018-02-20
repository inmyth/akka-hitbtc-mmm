package com.mbcu.hitbtc.mmm.models.internal

import play.api.libs.json.Json

object Order {
  implicit val jsonFormat = Json.format[Order]
}
case class Order (
  id : String,
  clientOrderId : String,
  symbol : String,
  side : String,
  status : String,
  `type` : String,
  timeInForce : String,
  quantity : String,
  price : String,
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