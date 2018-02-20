package com.mbcu.hitbtc.mmm.models.request

import play.api.libs.json.{JsValue, Json}

object SubscribeReports {

  def toJsValue() : JsValue = {
    val str: String =
      """
        |{
        | "method": "subscribeReports",
        | "id": "subscribeReports",
        | "params": {}
        |}
      """.stripMargin
    Json.parse(str)
  }
  
}


/*

{
  "method": "subscribeReports",
  "params": {}
}
 */