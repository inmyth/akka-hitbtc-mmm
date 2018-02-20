package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, Props}
import com.mbcu.hitbtc.mmm.actors.ParserActor.{GotWSResponse}
import com.mbcu.hitbtc.mmm.models.internal.Config

object ParserActor {
  def props(config: Option[Config]): Props = Props(new ParserActor(config))

  case class GotWSResponse(raw : String)
}


class ParserActor(config : Option[Config]) extends Actor {


  override def receive: Receive = {


    case GotWSResponse(raw : String) => {


    }
  }
}
