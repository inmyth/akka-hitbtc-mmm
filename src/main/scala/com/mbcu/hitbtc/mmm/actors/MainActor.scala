package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import akka.dispatch.ExecutionContexts._
import com.mbcu.hitbtc.mmm.actors.ParserActor.{ActiveOrders, LoginSuccess, RPCFailed, SubsribeReportsSuccess}
import com.mbcu.hitbtc.mmm.actors.WsActor._
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.request.{Login, SubscribeReports}
import com.mbcu.hitbtc.mmm.models.response.{Order, RPCError}
import com.sun.xml.internal.ws.api.Cancelable
import play.api.libs.json.Json

object MainActor {
  def props(configPath : String): Props = Props(new MainActor(configPath))

  case class ConfigInitiated(config : Config)

}

class MainActor(configPath : String) extends Actor{
  import com.mbcu.hitbtc.mmm.actors.MainActor._
  private var config: Option[Config] = None
  private var ws: Option[ActorRef] = None
  private var parser: Option[ActorRef] = None
  private var cancellable : Option[Cancellable] = None
  private var state : Option[ActorRef] = None
//  private var state : Option[State] = None
  implicit val ec = global
  val initDone : Boolean = false

  override def receive: Receive = {

    case "start" => {
        val fileActor = context.actorOf(Props(new FileActor(configPath)))
        fileActor ! "start"
    }

    case ConfigInitiated(cfg) => {
      config = Some(cfg)
      state = Some(context.actorOf(Props(new StateActor(cfg)), name = "state"))
      state foreach (_ ! "start")
      ws = Some(context.actorOf(Props(new WsActor("wss://api.hitbtc.com/api/2/ws")), name = "ws"))
//      self ! "init logger"
      ws.foreach(_ ! "start")
    }

    case "init logger" => {
      val scheduleActor = context.actorOf(Props(classOf[ScheduleActor]))
      cancellable =Some(
        context.system.scheduler.schedule(
          0 second,
          5 second,
          scheduleActor,
          "log orderbooks"))
    }

    case s : String if s == "log orderbooks" => state foreach (_ forward s)

    case WsConnected => {
      parser = Some(context.actorOf(Props(new ParserActor(config))))
      self ! "login"
    }

    case "login" => {
      config.foreach(c => ws.foreach(_ ! SendJs(Json.toJson(Login.from(c)))))
    }

    case LoginSuccess => ws.foreach(_ ! SendJs(SubscribeReports.toJsValue()))

    case SubsribeReportsSuccess => println("Subscribe Reports success")

    case activeOrders : ActiveOrders => state foreach (_ forward activeOrders)


    case RPCFailed(id, error) => {
      println(s"Id : ${id}, ${error}")
      context.system.terminate()
    }


    case wsGotText : WsGotText => {
      parser match {
        case Some(parserActor) => parserActor ! wsGotText
        case _ => {println("MainActor : Parser not available")}
      }
    }

  }



}
