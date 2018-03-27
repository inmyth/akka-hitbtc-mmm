package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef}
import com.mbcu.hitbtc.mmm.models.response.Order

import scala.collection.concurrent.TrieMap

class CacheActor extends Actor {
  var mainActor : Option[ActorRef] = None

  var cache : TrieMap[String, Order] = TrieMap.empty[String, Order]


  override def receive: Receive = {
    case "start" => mainActor = Some(sender())



  }

}
