package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, Props}
import com.mbcu.hitbtc.mmm.actors.ParserActor.GotResponse
import com.mbcu.hitbtc.mmm.models.internal.Config

object ParserActor {
  def props(config: Config): Props = Props(new ParserActor(config))

  case class GotResponse(raw : String)
}


class ParserActor(config : Config) extends Actor {

  override def receive: Receive = {

    case GotResponse(raw : String) => {

    }
  }
}
