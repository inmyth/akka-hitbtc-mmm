package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor.{Add, Sort}
import com.mbcu.hitbtc.mmm.actors.ParserActor.ActiveOrders
import com.mbcu.hitbtc.mmm.actors.StateActor.SmartSort
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.response.Order

object StateActor {
  def props(config: Config): Props = Props(new StateActor(config))

  case class SmartSort(ordersOption : Option[Seq[Order]])
}

class StateActor (val config : Config) extends Actor  {

  override def receive: Receive = {

    case "start" => {
      config.bots foreach(bot => context.actorOf(Props(new OrderbookActor(bot)), name = s"${bot.pair}"))
      context.children foreach(ob => println(ob.path))
    }

    case s : String if s == "log orderbooks" =>  context.children foreach  {_.forward(s)}



    case ActiveOrders(ordersOption : Option[Seq[Order]]) => {
      ordersOption match {
        case Some(orders) => orders foreach (order => {
          context.actorSelection(s"/user/main/state/${order.symbol}") ! Add(order)
        })
        case _ => println("MainActor#ActiveOrders : _")
      }
      self ! SmartSort(ordersOption)
    }

    case SmartSort(ordersOption : Option[Seq[Order]]) => {
      ordersOption match {
        case Some(orders) => {
          var res = Map.empty[String, String]
          orders foreach (order => {
            if (!res.contains(order.symbol)){
              res += (order.symbol -> order.side)
            }
            else {
              val e = res(order.symbol)
              if (e != order.side){
                res += (order.symbol -> "both")
              }
            }
          })
          res.foreach((x) =>  context.actorSelection(s"/user/main/state/${x._1}") ! Sort(x._2))
        }
        case _ => println("MainActor#ActiveOrders : _")
      }
//      if (buyUpd)
    }
  }



}
