package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.WsActor.WsGotText
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.request.SubscribeReports
import com.mbcu.hitbtc.mmm.models.response.{Order, RPCError}
import com.mbcu.hitbtc.mmm.utils.MyLogging
import play.api.libs.json.{JsDefined, JsValue, Json}

import scala.util.Try

object ParserActor extends MyLogging {
  def props(config: Option[Config]): Props = Props(new ParserActor(config))

  case object LoginSuccess

  case object SubsribeReportsSuccess

  case class ActiveOrders(orders : Option[Seq[Order]])


  case class OrderNew(order : Order)

  case class OrderFilled(order : Order)

  case class OrderPartiallyFilled(order : Order)

  case class OrderCancelled (order : Order)

  case class OrderSuspended(order : Order)

  case class OrderExpired(order : Order)

  case class ErrorAuthFailed(er : RPCError, id :Option[String])

  case class ErrorServer(er : RPCError, id :Option[String])

  case class ErrorSymbol(er : RPCError, id :Option[String])

  case class ErrorNotEnoughFund(er : RPCError, id :Option[String])

  case class ErrorCancelGhost(er : RPCError, id :Option[String])

  case class ErrorOrderTooSmall(er : RPCError, id :Option[String])

  case class ErrorNonAffecting (er : RPCError, id :Option[String])

}


class ParserActor(config : Option[Config]) extends Actor {
  import com.mbcu.hitbtc.mmm.actors.ParserActor._

  private var main : Option[ActorRef] = None

  override def receive: Receive = {

    case WsGotText(raw : String) => {
      info(
        s"""Raw response
          |$raw
        """.stripMargin)

      val jsValue : JsValue = Json.parse(raw)

      Try {
        if((jsValue \ "jsonrpc").isDefined){
          if ((jsValue \ "error").isDefined){
            val error = (jsValue \ "error").as[RPCError]
            val id = (jsValue \ "id").asOpt[String]
            error.code match {
              case 2011 | 10001 => sender() ! ErrorOrderTooSmall(error, id)
              case 20001 => sender() ! ErrorNotEnoughFund(error, id)
              case 20002 => sender() ! ErrorCancelGhost(error, id)
              case 2001 | 2002 => sender() ! ErrorSymbol(error, id)
              case 1001 | 1002 | 1003 | 1004 | 403 => sender() ! ErrorAuthFailed(error, id)
              case 429 | 500 | 503 | 504 => sender() ! ErrorServer(error, id)
              case _ => ErrorNonAffecting(error, id)
            }
          }
          else {
            if((jsValue \ "method").isDefined && (jsValue \ "params").isDefined){
              val method = (jsValue \ "method").as[String]
              method match {
                case "activeOrders" => sender() ! ActiveOrders((jsValue \ "params").asOpt[Seq[Order]])
                case "report" =>
                  val order = (jsValue \ "params").as[Order]
                  order.status match {
                    case "new" => sender() ! OrderNew(order)
                    case "filled" => sender ! OrderFilled(order)
                    case "partiallyFilled" => sender() ! OrderPartiallyFilled(order)
                    case "canceled" => sender() ! OrderCancelled(order)
                    case "expired" => sender() ! OrderExpired(order)
                    case "suspended" => sender() ! OrderSuspended(order)
                    case _ => println(s"ParserActor#WsGotTex unhandled reportType ${order.reportType}")
                  }
                case _ => println("ParserActor#WsGotText _ " + method)
              }
            }
            else if ((jsValue \ "id").isDefined){
              val id : String = (jsValue \ "id").as[String]
              id match {
                case "login" =>  sender() ! LoginSuccess
                case "subscribeReports" =>  sender() ! SubsribeReportsSuccess
              }
            }
          }
        }
      }

    }
  }
}

/*

403 	401 	Action is forbidden for account
429 	429 	Too many requests 	Action is being rate limited for account
500 	500 	Internal Server Error
503 	503 	Service Unavailable 	Try it again later
504 	504 	Gateway Timeout 	Check the result of your request later
1001 	401 	Authorisation required
1002 	401 	Authorisation failed
1003 	403 	Action is forbidden for this API key 	Check permissions for API key
1004 	401 	Unsupported authorisation method 	Use Basic authentication
2001 	400 	Symbol not found
2002 	400 	Currency not found
20001 	400 	Insufficient funds 	Insufficient funds for creating order or any account operation
20002 	400 	Order not found 	Attempt to get active order that not existing, filled, canceled or expired. Attempt to cancel not existing order. Attempt to cancel already filled or expired order.
20003 	400 	Limit exceeded 	Withdrawal limit exceeded
20004 	400 	Transaction not found 	Requested transaction not found
20005 	400 	Payout not found
20006 	400 	Payout already committed
20007 	400 	Payout already rolled back
code":2011,"message":"Quantity too low
{"code":10001,"message":"\"price\" must be a positive number
code":2011,"message":"Quantity too low","description":"Minimum quantity 1"
 */