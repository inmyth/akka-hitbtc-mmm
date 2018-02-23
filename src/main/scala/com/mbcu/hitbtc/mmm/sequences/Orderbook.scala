package com.mbcu.hitbtc.mmm.sequences

import com.mbcu.hitbtc.mmm.models.response.Order
import play.api.libs.json.Json

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ListMap

class Orderbook (var pair: String, var sels : TrieMap[String, Order] = TrieMap.empty[String, Order], var buys : TrieMap[String, Order] = TrieMap.empty[String, Order], var sortedSels: scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order], var sortedBuys : scala.collection.immutable.Seq[Order] = scala.collection.immutable.Seq.empty[Order]) extends OrderbookTrait {


  override def sortBuys(): Seq[Order] = {
    buys.toSeq.sortWith(_._2.price > _._2.price).toMap.values.toSeq
  }

  override def sortSels(): Seq[Order] = {
    sels.toSeq.sortWith(_._2.price < _._2.price).toMap.values.toSeq
   }

  def add(order : Order) : Unit = {
    if(order.side == "buy"){
      buys put (order.id, order)
//      sortBuys()
    }else {
      sels put (order.id, order)
//      sortSels()
    }
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
