package com.mbcu.hitbtc.mmm

import com.mbcu.hitbtc.mmm.actors.MainActor


object Application extends App {

  import akka.actor.Props
  import com.mbcu.hitbtc.mmm.actors.FileActor

  override def main(args: Array[String]) {
    import system.dispatcher
    implicit val system = akka.actor.ActorSystem("mmm")
    implicit val materializer = akka.stream.ActorMaterializer()

    if (args.length != 1){
      println("Requires one argument : config file path")
      system.terminate();
    }
    var mainActor = system.actorOf(Props(new MainActor(args(0))), name = "main")
    mainActor ! "start"

//    val fileActor = system.actorOf(Props(new FileActor(args(0))))
//    fileActor ! "start"



  }

}
