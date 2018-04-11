package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor.Age.Age
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

  object Age extends Enumeration {
    type Age = Value
    val per, tra, all = Value
  }

}
class OrderbookActor (var bot : Bot) extends OrderbookTrait with Actor with MyLogging{
  var sels : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var buys : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var selTrans : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var buyTrans : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var sortedSels: scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var sortedBuys : scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var state : Option[ActorRef] = None
  var requestingTicker = false

  override def receive: Receive = {
    case "start" => state = Some(sender())

    case InitOrder(orders) =>
      orders.foreach(o => add(o, Age.per))
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
          remove(side, id, Age.per)
          remove(side, id, Age.tra)
          sort(side)
        case _ => warn(s"OrderbookActor#CancelInvalidOrder _ $id")
      }

    case OrderNew(order) =>
      newOrder(order)
      sort(order.side)
      val trans = if (order.side == Side.buy) buyTrans else selTrans
      if (trans.isEmpty){
        sendCancelOrders(trim(order.side))
      }

    case OrderFilled(order) =>
      removeTotal(order)

      val counters = counter(order)
      counters.foreach(o => add(newOrderToOrder(o), Age.tra))
      sort(Side.all)

      val growth = grow(order.side)
      growth.foreach(o => add(newOrderToOrder(o), Age.tra))

      sendOrders(counters, "counter")
      sendOrders(growth, "balancer")

    case OrderPartiallyFilled(order) =>
      newOrder(order)
      sort(order.side)

    case OrderCancelled(order) =>
      removeTotal(order)
      sort(order.side)
  }

  def newOrder(order : Order) : Unit = {
    add(order, Age.per)
    remove(order, Age.tra)
  }

  def removeTotal(order : Order) : Unit = {
    remove(order, Age.tra)
    remove(order, Age.per)
  }

  def sendOrders(no : Seq[NewOrder], as : String): Unit =state foreach (_ ! SendNewOrders(no, as))

  def sendCancelOrders(oc : Seq[Order]) : Unit = state foreach(_ ! SendCancelOrders(oc.map(o => CancelOrder(o.clientOrderId))))

  def add(order : Order, age :Age) : Unit = {
    var l = (age, order.side) match {
      case (Age.per, Side.buy) => buys
      case (Age.per, Side.sell) => sels
      case (Age.tra, Side.buy) => buyTrans
      case (Age.tra, Side.sell) => selTrans
      case _ => TrieMap.empty[String, Order]
    }
    l += (order.clientOrderId -> order)
  }

  def newOrderToOrder(order : NewOrder) : Order = Order(order.id, order.id, order.params.symbol, order.params.side, "fake", "fake", "fake", order.params.quantity, order.params.price, BigDecimal(0), "fake", "fake", None, None, "fake", None, None, None, None, None)

  def remove(order : Order, age : Age) : Unit = remove(order.side, order.clientOrderId, age)

  def remove(side : Side, clientOrderId : String, age : Age ) : Unit = {
    var l = (age, side) match {
      case (Age.per, Side.buy) => buys
      case (Age.per, Side.sell) => sels
      case (Age.tra, Side.buy) => buyTrans
      case (Age.tra, Side.sell) => selTrans
      case _ => TrieMap.empty[String, Order]
    }
    l -= clientOrderId
  }


  def sort(side : Side) : Unit = {
    side match {
      case Side.buy => sortedBuys = sortBuys(buys, buyTrans)
      case Side.sell => sortedSels = sortSels(sels, selTrans)
      case _ =>
        sortedBuys = sortBuys(buys, buyTrans)
        sortedSels = sortSels(sels, selTrans)
    }

  }

  def counter(order : Order) : Seq[NewOrder] = Strategy.counter(order.quantity, order.price,
    bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.gridSpace, order.side, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)


  def trim(side : Side) : Seq[Order] = {
    val (orders, limit) = if (side == Side.buy) (sortedBuys, bot.buyGridLevels) else (sortedSels, bot.sellGridLevels)
    orders.slice(limit, orders.size)
  }

  def grow(side : Side) : Seq[NewOrder] = {
    def matcher(side : Side) : Seq[NewOrder] = {
      val preSeed = getRuntimeSeedStart(side)
      Strategy.seed(preSeed._2, preSeed._3, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, preSeed._1, bot.gridSpace, side, preSeed._4, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
    }
    side match {
      case Side.buy  => matcher(Side.buy)
      case Side.sell => matcher(Side.sell)
      case _ =>  Seq.empty[NewOrder] // unsupported operation at runtime
    }
  }

//  def saveSeed(side : Side, o : NewOrder) : Unit = {
//    side match {
//      case Side.buy => buys.put(o.params.clientOrderId, Order.tempNewOrder(o))
//      case Side.sell => sels.put(o.params.clientOrderId, Order.tempNewOrder(o))
//      case _ => println("OrderbookActor#seed _")
//    }
//  }

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

  def getRuntimeSeedStart(side : Side) : (Int, BigDecimal, BigDecimal, Boolean) = {
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

   def sortBuys(buys : TrieMap[String, Order], buyTrans : TrieMap[String, Order]) : scala.collection.immutable.Seq[Order] =
     collection.immutable.Seq((buys ++ buyTrans).toSeq.map(_._2).sortWith(_.price > _.price) : _*)


   def sortSels(sels : TrieMap[String, Order], selTrans : TrieMap[String, Order]): scala.collection.immutable.Seq[Order] =
     collection.immutable.Seq((sels ++ selTrans).toSeq.map(_._2).sortWith(_.price < _.price) : _*)


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
