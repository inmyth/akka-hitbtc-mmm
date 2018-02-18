package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import akka.dispatch.ExecutionContexts._
import com.mbcu.hitbtc.mmm.actors.WsActor.WsConnected
import com.mbcu.hitbtc.mmm.models.internal.Config

object MainActor {
  def props(configPath : String): Props = Props(new MainActor(configPath))

  case class ConfigInitiated(config : Config)

}

class MainActor(configPath : String) extends Actor{
  import com.mbcu.hitbtc.mmm.actors.MainActor._
  private var config: Option[Config] = None
  private var wsActor: Option[ActorRef] = None

  implicit val ec = global

  override def receive: Receive = {

    case "start" => {
        val fileActor = context.actorOf(Props(new FileActor(configPath)))
        val fileF = fileActor ! "start"
    }

    case ConfigInitiated(cfg) => {
      config = Some(cfg)
      wsActor = Some(context.actorOf(Props(new WsActor("wss://api.hitbtc.com/api/2/ws"))))
      wsActor.map(_ ! "start")
    }

    case WsConnected => {
      config match {
        case Some(c) => {
          c.bots.foreach(b => {
            println(b)
          })
        }
        case None => {
          println("config empty")
          context.system.terminate()
        }
      }


    }

  }



}
