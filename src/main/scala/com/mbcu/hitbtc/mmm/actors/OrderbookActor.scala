package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor.{CancelInvalidOrder, InitOrder, Sort}
import com.mbcu.hitbtc.mmm.actors.ParserActor.{OrderCancelled, OrderFilled, OrderNew, OrderPartiallyFilled}
import com.mbcu.hitbtc.mmm.actors.StateActor.SendNewOrder
import com.mbcu.hitbtc.mmm.models.internal.{Bot, Config}
import com.mbcu.hitbtc.mmm.models.request.NewOrder
import com.mbcu.hitbtc.mmm.models.response.Order
import com.mbcu.hitbtc.mmm.sequences.Strategy
import com.mbcu.hitbtc.mmm.traits.OrderbookTrait
import com.mbcu.hitbtc.mmm.utils.{MyLogging, MyUtils}

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ListMap


object OrderbookActor {
  def props(bot : Bot): Props = Props(new OrderbookActor(bot))

  case class InitOrder(order : Order)

  case class Sort(side : String)

  case class CancelInvalidOrder(clientOrderId : String)

}
class OrderbookActor (var bot : Bot) extends OrderbookTrait with Actor with MyLogging{
  var sels : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var buys : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var sortedSels: scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var sortedBuys : scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var state : Option[ActorRef] = None

  override def receive: Receive = {
    case "start" => state = Some(sender())

    case InitOrder(order) => add(order)

    case "init orders completed" =>
      sort("all")
      balancer("all") foreach sendOrder

    case "log orderbooks" => info(dump())

    case Sort(side) => sort("all")

    case CancelInvalidOrder(id) =>
      MyUtils.sideFromId(id) match {
        case Some(side) =>
          remove(side, id)
          sort(side)
        case _ => warn(s"OrderbookActor#CancelInvalidOrder _ $id")
      }

    case OrderNew(order) =>
      add(order)
      sort(order.side)

    case OrderFilled(order) =>
      remove(order)
      var newOrders = counter(order)
      newOrders ++= balancer(order.side)
      newOrders foreach sendOrder

    case OrderPartiallyFilled(order) =>
      add(order)
      sort(order.side)

    case OrderCancelled(order) =>
      remove(order)
      sort(order.side)
  }

  def sendOrder(no : NewOrder): Unit ={
    state foreach (_ ! SendNewOrder(no))
  }

  def add(order : Order) : Unit = {
    order.side match {
        case "buy" => buys += (order.clientOrderId -> order)
        case "sell" => sels += (order.clientOrderId -> order)
        case _ => warn(s"OrderbookActor#add unrecognized side ${order.side}")
    }
  }

  def remove(side : String, id : String) : Unit = {
    side match {
      case "buy" => buys.remove(id)
      case "sell" => sels.remove(id)
      case _ => warn(s"OrderbookActor#CancelInvalidOrder _ _ $id")
    }
  }

  def remove(order : Order) : Unit = {
    order.side match {
      case "buy" => buys -=  order.clientOrderId
      case "sell" => sels -= order.clientOrderId
      case _ => warn(s"OrderbookActor#remove unrecognized side ${order.side}")
    }
  }


  def sort(side : String) : Unit = {
    side match {
      case "buy" => sortedBuys = sortBuys(buys)
      case "sell" => sortedSels = sortSels(sels)
      case _ =>
        sortedBuys = sortBuys(buys)
        sortedSels = sortSels(sels)
    }

  }

  def counter(order : Order) : Seq[NewOrder] = {
    Strategy.counter(order.quantity, order.price, order.symbol, bot.gridSpace, order.side, bot.strategy )
  }

  def balancer(side : String) : Seq[NewOrder] = {
    var newOrders : Seq[NewOrder] = Seq.empty

    def matcher(side : String) : Seq[NewOrder] = {
      side match {
        case "buy" =>
          val buySeed = seed("buy")
          buySeed.foreach(no => saveSeed("buy", no))
          buySeed
        case "sell" =>
          val selSeed = seed("sell")
          selSeed.foreach(no => saveSeed("sell", no))
          selSeed
      }
    }
    side match {
      case "buy"  => newOrders ++= matcher("buy")
      case "sell" => newOrders ++= matcher("sell")
      case _ =>
        newOrders ++= matcher("buy")
        newOrders ++= matcher("sell")
    }
    newOrders
  }

  def seed(side : String) : Seq[NewOrder] = {
    val preSeed = getPreSeed(side)
    if (preSeed._1 > 0){
      Strategy.seed(preSeed._2, preSeed._3, bot.pair, preSeed._1, bot.gridSpace, side, preSeed._4, bot.strategy)
    }
    else {
      Seq.empty[NewOrder]
    }
  }

  def saveSeed(side : String, o : NewOrder) : Unit = {
      side match {
        case "buy" => buys.put(o.params.clientOrderId, Order.tempNewOrder(o))
        case "sell" => sels.put(o.params.clientOrderId, Order.tempNewOrder(o))
        case _ => println("OrderbookActor#seed _")
      }

  }

  def getPreSeed(side : String) : (Int, BigDecimal, BigDecimal, Boolean) = {
    var qty0 : BigDecimal = BigDecimal("0")
    var unitPrice0 : BigDecimal = BigDecimal("0")
    var isPulledFromOtherSide : Boolean = false
    var levels : Int = 0

    if (sortedBuys.isEmpty && sortedSels.isEmpty){
       qty0 = if (side == "buy") bot.buyOrderQuantity else bot.sellOrderQuantity
       unitPrice0 = bot.startMiddlePrice
       levels = if (side == "buy") bot.buyGridLevels else bot.sellGridLevels
    }
    else {
      side match {
        case "buy" =>
          if (sortedBuys.isEmpty){
            levels = bot.buyGridLevels
            getTopSel foreach(o => {
              qty0 = o.quantity
              unitPrice0 = o.price
              isPulledFromOtherSide = true
            })
          }
          else {
            levels = bot.buyGridLevels - sortedBuys.size
            getLowBuy foreach(o => {
              qty0 = o.quantity
              unitPrice0 = o.price
            })
          }
        case "sell" =>
          if (sortedSels.isEmpty){
            levels = bot.sellGridLevels
            getTopBuy foreach(o => {
              qty0 = o.quantity
              unitPrice0 = o.price
              isPulledFromOtherSide = true
            })
          }
          else {
            levels = bot.sellGridLevels - sortedSels.size
            getLowSel foreach(o => {
              qty0 = o.quantity
              unitPrice0 = o.price
            })
          }
        case _ => println("Orderbookactor#getPreSeed : _")
      }
    }
    (levels, qty0, unitPrice0, isPulledFromOtherSide)
  }

   def sortBuys(buys : TrieMap[String, Order]) : scala.collection.immutable.Seq[Order] = {
     collection.immutable.Seq(buys.toSeq.map(_._2).sortWith(_.price > _.price) : _*)
  }

   def sortSels(sels : TrieMap[String, Order]): scala.collection.immutable.Seq[Order] = {
     collection.immutable.Seq(sels.toSeq.map(_._2).sortWith(_.price < _.price) : _*)
   }


  def addBuy(order : Order) : Unit = buys put (order.id, order)

  def addSel(order : Order) : Unit = sels put (order.id, order)

  override def getTopSel: Option[Order] = sortedSels.lift(0)

  override def getLowSel: Option[Order] = sortedSels.lastOption

  override def getTopBuy: Option[Order] = sortedBuys.lift(0)

  override def getLowBuy: Option[Order] = sortedBuys.lastOption

   def dump() : String = {
    val builder = StringBuilder.newBuilder
    builder.append(System.getProperty("line.separator"))
    builder.append(s"buys : ${buys.size}")
    builder.append(System.getProperty("line.separator"))
    sortedBuys.foreach(b => {
      builder.append(s"quantity:${b.quantity} price:${b.price} filled:${b.cumQuantity}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.append(s"sells : ${sels.size}")
    builder.append(System.getProperty("line.separator"))
    sortedSels.foreach(s => {
      builder.append(s"quantity:${s.quantity} price:${s.price} filled:${s.cumQuantity}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.toString()
  }


}
