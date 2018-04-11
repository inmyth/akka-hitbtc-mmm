package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor._
import com.mbcu.hitbtc.mmm.actors.ParserActor._
import com.mbcu.hitbtc.mmm.actors.StateActor.{ReqTick, SendCancelOrders, SendNewOrders, UnreqTick}
import com.mbcu.hitbtc.mmm.models.internal.Bot
import com.mbcu.hitbtc.mmm.models.request.{CancelOrder, NewOrder}
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.models.response.{Order, Side}
import com.mbcu.hitbtc.mmm.sequences.Strategy
import com.mbcu.hitbtc.mmm.traits.OrderbookTrait
import com.mbcu.hitbtc.mmm.utils.{MyLogging, MyUtils}

import scala.collection.concurrent.TrieMap


object OrderbookActor {
  def props(bot : Bot): Props = Props(new OrderbookActor(bot))

  case class InitOrder(orders : Seq[Order])

  case class Sort(side : Side)

  case class Trim(side : Side)

  case class CancelInvalidOrder(clientOrderId : String)


}
class OrderbookActor (var bot : Bot) extends OrderbookTrait with Actor with MyLogging{
  var sels : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var buys : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var sortedSels: scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var sortedBuys : scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var state : Option[ActorRef] = None
  var requestingTicker = false

  override def receive: Receive = {
    case "start" => state = Some(sender())

    case InitOrder(orders) =>
      orders foreach add
      sort(Side.all)
      requestingTicker = true
      state foreach(_ ! ReqTick(bot.pair))

    case "log orderbooks" => info(dump())

    case Trim(side) =>  sendCancelOrders(trim(side))

    case GotTicker(ticker)  =>
      if (requestingTicker){
        requestingTicker = false
        sendOrders(initialSeed(ticker.last), "seed")
        sendCancelOrders(sortedSels ++ sortedBuys)
        state foreach(_ ! UnreqTick(bot.pair))
      }

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
      sendCancelOrders(trim(order.side))

    case OrderFilled(order) =>
      remove(order)
      sort(order.side)
      sendOrders(counter(order), "counter")
      sendOrders(grow(order.side), "balancer")

    case OrderPartiallyFilled(order) =>
      add(order)
      sort(order.side)

    case OrderCancelled(order) =>
      remove(order)
      sort(order.side)
  }

  def sendOrders(no : Seq[NewOrder], as : String): Unit =state foreach (_ ! SendNewOrders(no, as))

  def sendCancelOrders(oc : Seq[Order]) : Unit = state foreach(_ ! SendCancelOrders(oc.map(o => CancelOrder(o.clientOrderId))))


  def add(order : Order) : Unit = {
    order.side match {
        case Side.buy => buys += (order.clientOrderId -> order)
        case Side.sell => sels += (order.clientOrderId -> order)
        case _ => warn(s"OrderbookActor#add unrecognized side ${order.side}")
    }
  }

  def remove(side : Side, id : String) : Unit = {
    side match {
      case Side.buy => buys.remove(id)
      case Side.sell => sels.remove(id)
      case _ => warn(s"OrderbookActor#CancelInvalidOrder $id")
    }
  }

  def remove(order : Order) : Unit = {
    order.side match {
      case Side.buy => buys -=  order.clientOrderId
      case Side.sell => sels -= order.clientOrderId
      case _ => warn(s"OrderbookActor#remove unrecognized side ${order.side}")
    }
  }


  def sort(side : Side) : Unit = {
    side match {
      case Side.buy => sortedBuys = sortBuys(buys)
      case Side.sell => sortedSels = sortSels(sels)
      case _ =>
        sortedBuys = sortBuys(buys)
        sortedSels = sortSels(sels)
    }

  }

  def counter(order : Order) : Seq[NewOrder] = Strategy.counter(order.quantity, order.price,
    bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.gridSpace, order.side, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)


  def trim(side : Side) : Seq[Order] = {
    val (orders, limit) = if (side == Side.buy) (sortedBuys, bot.buyGridLevels) else (sortedSels, bot.sellGridLevels)
    orders.slice(limit, orders.size)
  }

  def grow(side : Side) : Seq[NewOrder] = {
    var newOrders : Seq[NewOrder] = Seq.empty

    def matcher(side : Side) : Seq[NewOrder] = {
      val seeds = seed(side)
      seeds.foreach(no => saveSeed(side, no))
      seeds
    }
    side match {
      case Side.buy  => newOrders ++= matcher(Side.buy)
      case Side.sell => newOrders ++= matcher(Side.sell)
      case _ =>
        newOrders ++= matcher(Side.buy)
        newOrders ++= matcher(Side.sell)
    }
    newOrders
  }

  def seed(side : Side) : Seq[NewOrder] = {
    val preSeed = getPreSeed(side)
    Strategy.seed(preSeed._2, preSeed._3, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, preSeed._1, bot.gridSpace, side, preSeed._4, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
  }

  def saveSeed(side : Side, o : NewOrder) : Unit = {
    side match {
      case Side.buy => buys.put(o.params.clientOrderId, Order.tempNewOrder(o))
      case Side.sell => sels.put(o.params.clientOrderId, Order.tempNewOrder(o))
      case _ => println("OrderbookActor#seed _")
    }
  }

  def initialSeed(midPrice : BigDecimal): Seq[NewOrder] = {
    var res : Seq[NewOrder] = Seq.empty[NewOrder]

    var buyQty = BigDecimal(0)
    var selQty = BigDecimal(0)
    var calcMidPrice = midPrice

    (sortedBuys.size, sortedSels.size) match {
      case (a, s) if a == 0 && s == 0 =>
        buyQty = bot.buyOrderQuantity
        selQty = bot.sellOrderQuantity
        calcMidPrice = midPrice

      case (a, s) if a != 0 && s == 0 =>
        val anyBuy  = sortedBuys.head
        val calcMid = Strategy.calcMid(anyBuy.price, anyBuy.quantity, bot.quantityPower, bot.gridSpace, bot.counterScale, Side.buy, midPrice, bot.strategy)
        buyQty = calcMid._2
        selQty = calcMid._2
        calcMidPrice = calcMid._1

      case (a, s) if a == 0 && s != 0 =>
        val anySel  = sortedSels.head
        val calcMid = Strategy.calcMid(anySel.price, anySel.quantity, bot.quantityPower, bot.gridSpace, bot.counterScale, Side.sell, midPrice, bot.strategy)
        buyQty = calcMid._2
        selQty = calcMid._2
        calcMidPrice = calcMid._1

      case (a, s) if a != 0 && s != 0 =>
        val anySel  = sortedSels.head
        val calcMid = Strategy.calcMid(anySel.price, anySel.quantity, bot.quantityPower, bot.gridSpace, bot.counterScale, Side.sell, midPrice, bot.strategy)
        buyQty = calcMid._2
        selQty = calcMid._2
        calcMidPrice = calcMid._1
    }

    res ++= Strategy.seed(buyQty, calcMidPrice, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.buyGridLevels, bot.gridSpace, Side.buy, false, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
    res ++= Strategy.seed(buyQty, calcMidPrice, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.sellGridLevels, bot.gridSpace, Side.sell, false, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
    res
  }

  def getPreSeed(side : Side) : (Int, BigDecimal, BigDecimal, Boolean) = {
    var qty0 : BigDecimal = BigDecimal("0")
    var unitPrice0 : BigDecimal = BigDecimal("0")
    var isPulledFromOtherSide : Boolean = false
    var levels : Int = 0

    var order : Option[Order] = None
    side match {
      case Side.buy =>
      sortedBuys.size match {
        case 0 =>
          levels = bot.buyGridLevels
          isPulledFromOtherSide = true
          order = Some(getTopSel)
        case _ =>
          levels = bot.buyGridLevels - sortedBuys.size
          order = Some(getLowBuy)
      }
      case Side.sell =>
        sortedSels.size match {
          case 0 =>
            levels = bot.sellGridLevels
            isPulledFromOtherSide = true
            order = Some(getTopBuy)
          case _ =>
            levels = bot.sellGridLevels - sortedSels.size
            order = Some(getLowSel)
        }
      case _ => println("Orderbookactor#getPreSeed : _")
    }
    order foreach (o => {
      qty0 = o.quantity
      unitPrice0 = o.price
    })
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

  override def getTopSel: Order = sortedSels.head

  override def getLowSel: Order = sortedSels.last

  override def getTopBuy: Order = sortedBuys.head

  override def getLowBuy: Order = sortedBuys.last

   def dump() : String = {
    val builder = StringBuilder.newBuilder
    builder.append(System.getProperty("line.separator"))
    builder.append(self.path.name)
     builder.append(System.getProperty("line.separator"))
     builder.append(s"buys : ${buys.size}")
    builder.append(System.getProperty("line.separator"))
    sortedBuys.foreach(b => {
      builder.append(s"id:${b.clientOrderId} quantity:${b.quantity} price:${b.price} filled:${b.cumQuantity}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.append(s"sells : ${sels.size}")
    builder.append(System.getProperty("line.separator"))
    sortedSels.foreach(s => {
      builder.append(s"id:${s.clientOrderId} quantity:${s.quantity} price:${s.price} filled:${s.cumQuantity}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.toString()
  }


}
