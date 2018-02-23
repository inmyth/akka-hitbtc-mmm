package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.ParserActor._
import com.mbcu.hitbtc.mmm.actors.WsActor.WsGotText
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.request.SubscribeReports
import com.mbcu.hitbtc.mmm.models.response.{Order, RPC, RPCError}
import play.api.libs.json.{JsDefined, JsValue, Json}

import scala.util.Try

object ParserActor {
  def props(config: Option[Config]): Props = Props(new ParserActor(config))

  case object LoginSuccess

  case object SubsribeReportsSuccess

  case class ActiveOrders(orders : Option[Seq[Order]])

  case class RPCFailed(id : String, error : String)

  case class OrderNew(order : Order)

  case class OrderFilled(order : Order)

  case class OrderPartiallyFilled(order : Order)

  case class OrderCancelled (order : Order)

  case class OrderSuspended(order : Order)

  case class OrderExpired(order : Order)


}


class ParserActor(config : Option[Config]) extends Actor {
  private var main : Option[ActorRef] = None

  override def receive: Receive = {

    case WsGotText(raw : String) => {
      println(raw)

      val jsValue : JsValue = Json.parse(raw)

      Try {
        if((jsValue \ "jsonrpc").isInstanceOf[JsDefined]){
          if ((jsValue \ "error").isInstanceOf[JsDefined]){
            sender() ! RPCFailed((jsValue \ "id").as[String], (jsValue \ "error").toString)
          }
          else {
            if((jsValue \ "method").isDefined && (jsValue \ "params").isDefined){
              val method = (jsValue \ "method").as[String]
              if (method == "activeOrders"){ // params returns array
                sender() ! ActiveOrders((jsValue \ "params").asOpt[Seq[Order]])
              }
              else if (method == "report") { // order new, filled, partially-filled
                val order = (jsValue \ "params").as[Order]
                order.status match {
                  case "new" => sender() ! OrderNew(order)
                  case "filled" => sender ! OrderFilled(order)
                  case "partiallyFilled" => sender() ! OrderPartiallyFilled(order)
                  case "canceled" => sender() ! OrderCancelled(order)
                  case _ => println(s"ParserActor : Unhandled report type ${order.reportType}")
                }
              }

            }
            else if ((jsValue \ "id").isDefined){
              if ((jsValue \ "id").as[String] == "login"){
                sender() ! LoginSuccess
              } else if((jsValue \ "id").as[String] == "subscribeReports") {
                sender() ! SubsribeReportsSuccess
              }
            }
          }
        }
      }

    }
  }
}
