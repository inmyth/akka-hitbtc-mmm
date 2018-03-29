package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor.{CancelInvalidOrder, InitOrder, Sort}
import com.mbcu.hitbtc.mmm.actors.ParserActor._
import com.mbcu.hitbtc.mmm.actors.StateActor.{SendNewOrder, SmartSort}
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.request.NewOrder
import com.mbcu.hitbtc.mmm.models.response.{Order, Side}
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.utils.{MyLogging, MyUtils}

object StateActor {
  def props(config: Config): Props = Props(new StateActor(config))

  case class SmartSort(ordersOption : Option[Seq[Order]])

  case class SendNewOrder(no : NewOrder, as : String)
}

class StateActor (val config : Config) extends Actor with MyLogging  {
  private var main : Option[ActorRef] = None

  override def receive: Receive = {

    case "start" =>
      main = Some(sender())
      config.bots foreach(bot => context.actorOf(Props(new OrderbookActor(bot)), name = s"${bot.pair}"))
      context.children foreach(_ ! "start")

    case s : String if s == "log orderbooks" =>  context.children foreach  {_.forward(s)}


    case ActiveOrders(ordersOption : Option[Seq[Order]]) =>
      ordersOption match {
        case Some(orders) => orders foreach (order => context.actorSelection(s"/user/main/state/${order.symbol}") ! InitOrder(ordersOption))
        case _ => println("MainActor#ActiveOrders : _")
      }

    case SmartSort(ordersOption : Option[Seq[Order]]) =>
      ordersOption match {
        case Some(orders) =>
          var res = Map.empty[String, Side]
          orders foreach (order => {
            if (!res.contains(order.symbol)){
              res += (order.symbol -> order.side)
            }
            else {
              val e = res(order.symbol)
              if (e != order.side){
                res += (order.symbol -> Side.all)
              }
            }
          })
          res.foreach((x) =>  context.actorSelection(s"/user/main/state/${x._1}") ! Sort(x._2))

        case _ => println("MainActor#ActiveOrders : _")
      }

    case SendNewOrder(newOrder, as) => main foreach (_ ! SendNewOrder(newOrder, as))

    case OrderNew(order) => context.actorSelection(s"/user/main/state/${order.symbol}") ! OrderNew(order)

    case OrderCancelled(order) => context.actorSelection(s"/user/main/state/${order.symbol}") ! OrderCancelled(order)

    case OrderPartiallyFilled(order) => context.actorSelection(s"/user/main/state/${order.symbol}") ! OrderPartiallyFilled(order)

    case OrderFilled(order) => context.actorSelection(s"/user/main/state/${order.symbol}") ! OrderFilled(order)

    case OrderExpired(order) =>  context.actorSelection(s"/user/main/state/${order.symbol}") ! OrderCancelled(order)

    case ErrorNotEnoughFund(_, id) => cancelInvalidOrderFromId(id)

    case ErrorOrderTooSmall(_, id) => cancelInvalidOrderFromId(id)

    case ErrorCancelGhost(_, id) => cancelInvalidOrderFromId(id)

  }

  def cancelInvalidOrderFromId(idOpt : Option[String]) : Unit = {
    idOpt match {
      case Some(id) =>
        MyUtils.symbolFromId(id) match {
          case Some(symbol) => context.actorSelection(s"/user/main/state/$symbol") ! CancelInvalidOrder(id)
          case _ => warn(s"StateActor#cancelFromID no symbol for id : $id")
        }
      case _ => warn(s"StateActor#cancelFromID no id : $idOpt")
    }

  }


}
