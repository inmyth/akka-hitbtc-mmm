package com.mbcu.hitbtc.mmm.models.response

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.FunSuite
import play.api.libs.json.Json

class TickerTest extends FunSuite{

  val s: String = """{
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
    assert(params.bid === BigDecimal("0.000001940"))
    assert(params.last === BigDecimal("0.000001950"))
    assert(params.open === BigDecimal("0.000002300"))
    assert(params.low === BigDecimal("0.000001700"))
    assert(params.high === BigDecimal("0.000002359"))
    assert(params.volume === BigDecimal("159013000"))
    assert(params.volumeQuote === BigDecimal("311.501319"))
    assert(params.symbol === "NOAHBTC")
    assert(params.timestamp === DateTime.parse("2018-03-28T04:07:48.724Z").withZone(DateTimeZone.UTC))


  }

}
