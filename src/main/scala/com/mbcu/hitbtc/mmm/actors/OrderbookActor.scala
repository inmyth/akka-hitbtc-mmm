package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, Props}
import com.mbcu.hitbtc.mmm.actors.OrderbookActor.{Add, Sort}
import com.mbcu.hitbtc.mmm.models.internal.{Bot, Config}
import com.mbcu.hitbtc.mmm.models.response.Order
import com.mbcu.hitbtc.mmm.sequences.OrderbookTrait

import scala.collection.concurrent.TrieMap


object OrderbookActor {
  def props(bot : Bot): Props = Props(new OrderbookActor(bot))

  case class Add(order : Order)

  case class Sort(side : String)

}
class OrderbookActor (var bot : Bot) extends OrderbookTrait with Actor  {
  var sels : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var buys : TrieMap[String, Order] = TrieMap.empty[String, Order]
  var sortedSels: scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]
  var sortedBuys : scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]

  override def receive: Receive = {
    case Add(order) => {
      if(order.side == "buy"){
        buys put (order.id, order)
      }
      else {
        sels put (order.id, order)
      }
    }

    case "log orderbooks" => println(toString)

    case Sort(side) => {
      side match {
        case "buy"  => sortedBuys = sortBuys(buys)
        case "sell" => sortedSels = sortSels(sels)
        case _ =>
          sortedBuys = sortBuys(buys)
          sortedSels = sortSels(sels)
      }
    }
  }


   def sortBuys(buys : TrieMap[String, Order]) : scala.collection.immutable.Seq[Order] = {
    collection.immutable.Seq(buys.toSeq.sortWith(_._2.price > _._2.price).toMap.values.toSeq: _*)
  }

   def sortSels(sels : TrieMap[String, Order]): scala.collection.immutable.Seq[Order] = {
    collection.immutable.Seq(sels.toSeq.sortWith(_._2.price < _._2.price).toMap.values.toSeq: _*)
   }



  def addBuy(order : Order) : Unit = buys put (order.id, order)

  def addSel(order : Order) : Unit = sels put (order.id, order)

  override def getTopSel: Option[Order] = sortedSels.lift(0)

  override def getLowSel: Option[Order] = sortedSels.lastOption

  override def getTopBuy: Option[Order] = sortedBuys.lift(0)

  override def getLowBuy: Option[Order] = sortedBuys.lastOption

  override def toString : String = {
    val builder = StringBuilder.newBuilder

    builder.append(s"buys : ${buys.size}")
    builder.append(System.getProperty("line.separator"))
    sortedBuys.foreach(b => {
      builder.append(s"quantity:${b.quantity} price:${b.price}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.append(s"sells : ${sels.size}")
    builder.append(System.getProperty("line.separator"))
    sortedSels.foreach(s => {
      builder.append(s"quantity:${s.quantity} price:${s.price}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.toString()
  }

  def seed() : Unit = {
    if (buys.isEmpty && sels.isEmpty){

    }
  }


}
