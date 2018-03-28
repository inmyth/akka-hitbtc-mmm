package com.mbcu.hitbtc.mmm.models.response

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads

object Ticker {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val customReads = Reads[DateTime](js =>
    js.validate[String].map[DateTime](dtString =>
      DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat).withZone(DateTimeZone.UTC))
    )
  )


    implicit val tickerWrites: Writes[Ticker] = (
      (JsPath \ "symbol").write[String] and
        (JsPath \ "ask").write[BigDecimal] and
        (JsPath \ "bid").write[BigDecimal] and
        (JsPath \ "last").write[BigDecimal] and
        (JsPath \ "open").write[BigDecimal] and
        (JsPath \ "low").write[BigDecimal] and
        (JsPath \ "high").write[BigDecimal] and
        (JsPath \ "volume").write[BigDecimal] and
        (JsPath \ "volumeQuote").write[BigDecimal] and
        (JsPath \ "timestamp").write[DateTime](JodaWrites.jodaDateWrites(dateFormat))
      ) (unlift(Ticker.unapply))

    implicit val tickerReads: Reads[Ticker] = (
      (JsPath \ "symbol").read[String] and
        (JsPath \ "ask").read[BigDecimal] and
        (JsPath \ "bid").read[BigDecimal] and
        (JsPath \ "last").read[BigDecimal] and
        (JsPath \ "open").read[BigDecimal] and
        (JsPath \ "low").read[BigDecimal] and
        (JsPath \ "high").read[BigDecimal] and
        (JsPath \ "volume").read[BigDecimal] and
        (JsPath \ "volumeQuote").read[BigDecimal] and
        (JsPath \ "timestamp").read[DateTime](customReads)
      ) (Ticker.apply _)

    implicit val tickerFormat: Format[Ticker] = Format(tickerReads, tickerWrites)
}

case class Ticker(
     symbol: String,
     ask: BigDecimal,
     bid: BigDecimal,
     last: BigDecimal,
     open: BigDecimal,
     low: BigDecimal,
     high: BigDecimal,
     volume: BigDecimal,
     volumeQuote: BigDecimal,
     timestamp: DateTime
 )




/*
  "params": {
    "ask": "0.054464",
    "bid": "0.054463",
    "last": "0.054463",
    "open": "0.057133",
    "low": "0.053615",
    "high": "0.057559",
    "volume": "33068.346",
    "volumeQuote": "1832.687530809",
    "timestamp": "2017-10-19T15:45:44.941Z",
    "symbol": "ETHBTC"
  }
 */