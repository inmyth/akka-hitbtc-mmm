package com.mbcu.hitbtc.mmm.models.request

import com.mbcu.hitbtc.mmm.models.request.SubscribeMarket.Market.Market
import com.mbcu.hitbtc.mmm.sequences.Strategy.Strategies.Value
import play.api.libs.json.{JsValue, Json, Reads, Writes}

object SubscribeMarket {

  object Market extends Enumeration {
    type Market = Value
    val subscribeTicker, subscribeOrderbook, subscribeTrades, unsubscribeTicker, unsubscribeOrderbook, unsubscribeTrades = Value

    implicit val reads = Reads.enumNameReads(Market)
    implicit val writes = Writes.enumNameWrites
  }


  def apply(data : Market, symbol : String): JsValue = {
    val str: String =
      s"""
        |{
        | "method": "$data",
        | "id": "$data",
        | "params": {
        |   "symbol" : "$symbol"
        | }
        |}
      """.stripMargin
    Json.parse(str)
  }


}
