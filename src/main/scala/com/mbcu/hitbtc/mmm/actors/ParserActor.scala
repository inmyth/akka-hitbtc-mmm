package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.ParserActor.{ActiveOrders, LoginSuccess, RPCFailed, SubsribeReportsSuccess}
import com.mbcu.hitbtc.mmm.actors.WsActor.WsGotText
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.request.SubscribeReports
import com.mbcu.hitbtc.mmm.models.response.{Order, RPC, RPCError}
import play.api.libs.json.{JsDefined, JsValue, Json}

object ParserActor {
  def props(config: Option[Config]): Props = Props(new ParserActor(config))

  case object LoginSuccess

  case object SubsribeReportsSuccess

  case class ActiveOrders(orders : Option[List[Order]])

  case class RPCFailed(id : String, error : String)

}


class ParserActor(config : Option[Config]) extends Actor {
  private var main : Option[ActorRef] = None

  override def receive: Receive = {

    case WsGotText(raw : String) => {
      println(raw)

      val jsValue : JsValue = Json.parse(raw)
      if((jsValue \ "jsonrpc").isInstanceOf[JsDefined]){
        if ((jsValue \ "error").isInstanceOf[JsDefined]){
          sender() ! RPCFailed((jsValue \ "id").as[String], (jsValue \ "error").toString)
        }
        else {
          if((jsValue \ "method").isDefined && (jsValue \ "params").isDefined){
            val rpc = jsValue.as[RPC]
            if ((jsValue \ "method").as[String] == "activeOrders"){
              sender() ! ActiveOrders(rpc.params)
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
