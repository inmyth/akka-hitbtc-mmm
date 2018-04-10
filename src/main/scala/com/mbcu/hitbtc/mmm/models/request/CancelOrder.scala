package com.mbcu.hitbtc.mmm.models.request

import play.api.libs.json.{Json, OFormat}

case class CancelOrderParam(clientOrderId : String)
object CancelOrderParam {
  implicit val jsonFormat: OFormat[CancelOrderParam] = Json.format[CancelOrderParam]
}

case class CancelOrder(id :String, params: CancelOrderParam, method : String = "cancelOrder")
object CancelOrder {
  implicit val jsonFormat: OFormat[CancelOrder] = Json.format[CancelOrder]

  def apply(clientOrderId : String) : CancelOrder = {
    CancelOrder(clientOrderId, CancelOrderParam(clientOrderId))
  }
}
