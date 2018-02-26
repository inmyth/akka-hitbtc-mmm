package com.mbcu.hitbtc.mmm

import java.util.logging.Logger

import com.mbcu.hitbtc.mmm.actors.MainActor
import com.mbcu.hitbtc.mmm.models.response.RPCError
import com.mbcu.hitbtc.mmm.utils.{MyLogging, MyLoggingSingle, MyUtils}

import scala.collection.immutable.ListMap


object Application extends App with MyLogging {

  import akka.actor.Props
  import com.mbcu.hitbtc.mmm.actors.FileActor

  override def main(args: Array[String]) {
    import system.dispatcher
    implicit val system = akka.actor.ActorSystem("mmm")
    implicit val materializer = akka.stream.ActorMaterializer()

    if (args.length != 1){
      println("Requires one argument : config file path")
      System.exit(-1)
    }

    info(s"START UP ${MyUtils.date()}")

    var mainActor = system.actorOf(Props(new MainActor(args(0))), name = "main")
    mainActor ! "start"
  }

}
