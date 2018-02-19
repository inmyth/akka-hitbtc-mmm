package com.mbcu.hitbtc.mmm.models.internal

import play.api.libs.json.Json

case class SubscribeReports (method : String = "subscribeReports")
object SubscribeReports {
  implicit val jsonFormat = Json.format[SubscribeReports]
}


/*

{
  "method": "subscribeReports",
  "params": {}
}
 */