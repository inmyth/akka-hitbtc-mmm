package com.mbcu.hitbtc.mmm.models.response

import org.scalatest.FunSuite
import play.api.libs.json.Json

class TickerTest extends FunSuite{

  val s = """{
            |	"jsonrpc": "2.0",
            |	"method": "ticker",
            |	"params": {
            |		"ask": "0.000001949",
            |		"bid": "0.000001940",
            |		"last": "0.000001950",
            |		"open": "0.000002300",
            |		"low": "0.000001700",
            |		"high": "0.000002359",
            |		"volume": "159013000",
            |		"volumeQuote": "311.501319",
            |		"timestamp": "2018-03-28T04:07:48.724Z",
            |		"symbol": "NOAHBTC"
            |	}
            |}""".stripMargin

  test("Ticker test basic") {
    val json = Json.parse(s)
    val params = (json \ "params").as[Ticker]
    assert((json \ "method").as[String] === "ticker")
    assert(params.ask === BigDecimal("0.000001949"))



  }

}
