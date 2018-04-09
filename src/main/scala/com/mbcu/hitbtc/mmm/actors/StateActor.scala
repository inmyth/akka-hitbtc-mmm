package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor._
import com.mbcu.hitbtc.mmm.actors.ParserActor._
import com.mbcu.hitbtc.mmm.actors.StateActor.{ReqTick, SendNewOrders, SmartSort}
import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.request.NewOrder
import com.mbcu.hitbtc.mmm.models.response.{Order, Side}
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.utils.{MyLogging, MyUtils}

import scala.collection.mutable.ListBuffer

object StateActor {
  def props(config: Config): Props = Props(new StateActor(config))

  case class SmartSort(ordersOption : Option[Seq[Order]])

  case class SendNewOrders(no : Seq[NewOrder], as : String)

  case class SendCancelOrders(c : Seq[String])

  case class ReqTick(symbol : String)
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
        case Some(orders) =>
          val map = Map(config.bots.map(b => (b.pair, new ListBuffer[Order])): _*).withDefaultValue(new ListBuffer[Order])
          orders.foreach(order => map(order.symbol) += order)
          map foreach (t => context.actorSelection(s"/user/main/state/${t._1}") ! InitOrder(t._2))
        case _ => println("MainActor#ActiveOrders : _")
      }

    case InitCompleted(symbol) => main foreach(_ ! InitCompleted(symbol))

    case GotTicker(ticker) => context.actorSelection(s"/user/main/state/${ticker.symbol}") ! GotTicker(ticker)

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


    case SendNewOrders(newOrders, as) => main foreach (_ ! SendNewOrders(newOrders, as))

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
