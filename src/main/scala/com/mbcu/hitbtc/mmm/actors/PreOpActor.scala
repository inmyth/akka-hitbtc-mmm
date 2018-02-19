package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import akka.dispatch.ExecutionContexts._
import com.mbcu.hitbtc.mmm.models.internal.{Config, Login}
import play.api.libs.json.Json

import scala.concurrent.duration._

object PreOpActor {

  def props(config : Config, wsActor : ActorRef): Props = Props(new PreOpActor(config, wsActor))


}

class PreOpActor(config : Config, wsActor : ActorRef) extends Actor {

  implicit val ec = global
  override def receive: Receive = {

    case "start" => {
      self ! "login"
    }

    case "login" => {
      implicit val timeout = Timeout(25 seconds)
      val loginRequest = Login.from(config)
      val f = wsActor ? WsActor.SendSync(Json.toJson(loginRequest))
      f map {
        res => println("login " + res)
      }





    }
  }

}
