package com.mbcu.hitbtc.mmm.models.response

import akka.http.scaladsl.model.DateTime
import com.mbcu.hitbtc.mmm.models.request.NewOrder
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.sequences.Strategy.Strategies.Value
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Side extends Enumeration {
  type Side = Value
  val buy, sell, all = Value

  implicit val sideRead = Reads.enumNameReads(Side)
  implicit val sideWrite = Writes.enumNameWrites
  def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
}

object Order {
  implicit val jsonFormat = Json.format[Order]

  object Implicits {
    implicit val orderWrites = new Writes[Order] {
      def writes(order: Order): JsValue = Json.obj(
        "id" -> order.id,
        "clientOrderId" -> order.clientOrderId,
        "symbol" -> order.symbol,
        "side" -> order.side,
        "status" -> order.status,
        "type" -> order.`type`,
        "timeInForce" -> order.timeInForce,
        "quantity" -> order.quantity,
        "price" -> order.price,
        "cumQuantity" -> order.cumQuantity,
        "createdAt" -> order.createdAt,
        "updatedAt" -> order.updatedAt,
        "stopPrice" -> order.stopPrice,
        "expireTime" -> order.expireTime,
        "reportType" -> order.reportType,
        "tradeQuantity" -> order.tradeQuantity,
        "tradePrice" -> order.tradePrice,
        "tradeId" -> order.tradeId,
        "tradeFee" -> order.tradeFee,
        "originalRequestClientOrderId" -> order.originalRequestClientOrderId
      )
    }

    implicit val orderReads: Reads[Order] = (
        (JsPath \ "id").read[String] and
        (JsPath \ "clientOrderId").read[String] and
        (JsPath \ "symbol").read[String] and
        (JsPath \ "side").read[Side] and
        (JsPath \ "status").read[String] and
        (JsPath \ "type").read[String] and
        (JsPath \ "timeInForce").read[String] and
        (JsPath \ "quantity").read[BigDecimal] and
        (JsPath \ "price").read[BigDecimal] and
        (JsPath \ "cumQuantity").read[BigDecimal] and
        (JsPath \ "createdAt").read[String] and
        (JsPath \ "updatedAt").read[String] and
        (JsPath \ "stopPrice").readNullable[BigDecimal] and
        (JsPath \ "expireTime").readNullable[String] and
        (JsPath \ "reportType").read[String] and
        (JsPath \ "tradeQuantity").readNullable[BigDecimal] and
        (JsPath \ "tradePrice").readNullable[BigDecimal] and
        (JsPath \ "tradeId").readNullable[Long] and
        (JsPath \ "tradeFee").readNullable[BigDecimal] and
        (JsPath \ "originalRequestClientOrderId").readNullable[String]
      ) (Order.apply _)

  }

  def tempNewOrder(no : NewOrder) : Order = {
    val clientOrderId = no.params.clientOrderId
    val symbol = no.params.symbol
    val side = no.params.side
    val qty = no.params.quantity
    val price = no.params.price
    new Order("", clientOrderId, symbol, side, "", "", "", qty, price, BigDecimal("-1"), "", "", None, None, "")
  }
}

case class Order (
  id : String,
  clientOrderId : String,
  symbol : String,
  side : Side,
  status : String,
  `type` : String,
  timeInForce : String,
  quantity : BigDecimal,
  price : BigDecimal,
  cumQuantity : BigDecimal,
  createdAt : String,
  updatedAt : String,
  stopPrice : Option[BigDecimal] = None,
  expireTime : Option[String] = None,
  reportType : String,
  tradeQuantity : Option[BigDecimal] = None,
  tradePrice : Option[BigDecimal] = None ,
  tradeId : Option[Long] = None,
  tradeFee : Option[BigDecimal] = None ,
  originalRequestClientOrderId : Option[String] = None
)

/*
id 	Number 	Unique identifier for Order as assigned by exchange
clientOrderId 	String 	Unique identifier for Order as assigned by trader.
symbol 	String 	Trading symbol
side 	String 	sell or buy
status 	String 	new, suspended, partiallyFilled, filled, canceled, expired
type 	String 	Enum: limit, market, stopLimit, stopMarket
timeInForce 	String 	Time in force is a special instruction used when placing a trade to indicate how long an order will remain active before it is executed or expires
  GTC - Good till cancel. GTC order won't close until it is filled.
  IOC - An immediate or cancel order is an order to buy or sell that must be executed immediately, and any portion of the order that cannot be immediately filled is cancelled.
  FOK - Fill or kill is a type of time-in-force designation used in securities trading that instructs a brokerage to execute a transaction immediately and completely or not at all.
  Day - keeps the order active until the end of the trading day in UTC.
  GTD - Good till date specified in expireTime.
quantity 	Number 	Order quantity
price 	Number 	Order price
cumQuantity 	Number 	Cumulative executed quantity
createdAt 	Datetime
updatedAt 	Datetime
stopPrice 	Number
expireTime 	Datetime
reportType 	String 	One of: status, new, canceled, expired, suspended, trade, replaced
tradeId 	Number 	Required for reportType = trade. Trade Id.
tradeQuantity 	Number 	Required for reportType = trade. Quantity of trade.
tradePrice 	Number 	Required for reportType = trade. Trade price.
tradeFee 	Number 	Required for reportType = trade. Fee paid for trade.
originalRequestClientOrderId 	String 	Identifier of replaced order.
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