package com.mbcu.hitbtc.mmm.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.dispatch.ExecutionContexts.global
import com.mbcu.hitbtc.mmm.actors.OrderbookActor.Age.Age
import com.mbcu.hitbtc.mmm.actors.OrderbookActor._
import com.mbcu.hitbtc.mmm.actors.ParserActor._
import com.mbcu.hitbtc.mmm.actors.StateActor.{ReqTick, SendCancelOrders, SendNewOrders, UnreqTick}
import com.mbcu.hitbtc.mmm.models.internal.Bot
import com.mbcu.hitbtc.mmm.models.request.{CancelOrder, NewOrder}
import com.mbcu.hitbtc.mmm.models.response.Side.Side
import com.mbcu.hitbtc.mmm.models.response.{Order, PingPong, RPCError, Side}
import com.mbcu.hitbtc.mmm.sequences.Strategy
import com.mbcu.hitbtc.mmm.traits.OrderbookTrait
import com.mbcu.hitbtc.mmm.utils.{MyLogging, MyUtils}

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object OrderbookActor {
  def props(bot : Bot): Props = Props(new OrderbookActor(bot))

  case class InitOrder(orders : Seq[Order])

  case class Sort(side : Side)

  case class Trim(side : Side)

  case class CancelInvalidOrder(er : RPCError, id : String)

  case class NewInitOrders(orders : Seq[NewOrder])

  case class ReturnInvalidOrder(er :RPCError, order : Order)

  object Age extends Enumeration {
    type Age = Value
    val per, tra, all = Value
  }

}
class OrderbookActor (var bot : Bot) extends OrderbookTrait with Actor with MyLogging{
  implicit val ec: ExecutionContextExecutor = global

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

    case NewInitOrders(orders) =>
      orders.foreach(o => add(newOrderToOrder(o), Age.tra))
      sendOrders(orders, "seed")

    case Trim(side) =>  sendCancelOrders(trim(side))

    case GotTicker(ticker)  =>
      if (requestingTicker){
        requestingTicker = false
        context.system.scheduler.scheduleOnce(2 second, self, NewInitOrders(initialSeed(ticker.last)))
        sendCancelOrders(sortedSels ++ sortedBuys)
        state foreach(_ ! UnreqTick(bot.pair))
      }

    case CancelInvalidOrder(er, id) =>
      MyUtils.sideFromId(id) match {
        case Some(side) =>
          retrieve(side, id, Age.tra) match {
            case Some(o) => state foreach(_ ! ReturnInvalidOrder(er, o))
            case _ => warn(s"OrderbookActor#CancelInvalidOrder#retrieveTrans not found _ $id")
          }
          remove(side, id, Age.per)
          remove(side, id, Age.tra)
          sort(side)
        case _ => warn(s"OrderbookActor#CancelInvalidOrder _ $id")
      }

    case OrderNew(order) =>
      newOrder(order)
      sort(order.side)
      val trans = if (order.side == Side.buy) buyTrans else selTrans
      if (trans.isEmpty && bot.isStrictLevels){
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

  def retrieve(side : Side, id : String, age : Age) : Option[Order] = {
    val l = (age,side) match {
      case (Age.per, Side.buy) => buys
      case (Age.per, Side.sell) => sels
      case (Age.tra, Side.buy) => buyTrans
      case (Age.tra, Side.sell) => selTrans
      case _ => TrieMap.empty[String, Order]
    }
    l.get(id)
  }

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
    bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.gridSpace, order.side, MyUtils.pingpongFromId(order.id), bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)


  def trim(side : Side) : Seq[Order] = {
    val (orders, limit) = if (side == Side.buy) (sortedBuys, bot.buyGridLevels) else (sortedSels, bot.sellGridLevels)
    // this may cause a hole.
    orders.filter(o => o.id.contains(PingPong.ping.toString)).slice(limit, orders.size)
  }

  def grow(side : Side) : Seq[NewOrder] = {
    def matcher(side : Side) : Seq[NewOrder] = {
      val preSeed = getRuntimeSeedStart(side)
      Strategy.seed(preSeed._2, preSeed._3, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, preSeed._1, bot.gridSpace, side, PingPong.ping, preSeed._4, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
    }
    side match {
      case Side.buy  => matcher(Side.buy)
      case Side.sell => matcher(Side.sell)
      case _ =>  Seq.empty[NewOrder] // unsupported operation at runtime
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

    res ++= Strategy.seed(buyQty, calcMidPrice, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.buyGridLevels, bot.gridSpace, Side.buy, PingPong.ping,false, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
    res ++= Strategy.seed(selQty, calcMidPrice, bot.quantityPower, bot.counterScale, bot.baseScale, bot.pair, bot.sellGridLevels, bot.gridSpace, Side.sell, PingPong.ping,false, bot.strategy, bot.isNoQtyCutoff, bot.maxPrice, bot.minPrice)
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
