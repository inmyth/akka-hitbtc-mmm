package com.mbcu.hitbtc.mmm.sequences

import com.mbcu.hitbtc.mmm.models.response.Order
import play.api.libs.json.Json

class Orderbook (var pair: String, var sels : Seq[Order], var buys : Seq[Order]) extends OrderbookTrait {

  override def sort: Unit = {
    sortBuy
    sortSel
  }

  override def sortBuy: Unit = {
    buys = buys sortWith(_.price > _.price)
  }

  override def sortSel: Unit = {
    sels = sels sortWith(_.price < _.price)
  }

  def add(order : Order) : Unit = {
    if(order.side == "buy"){
      buys = buys :+ order
      sortBuy
    }else {
      sels = sels :+ order
      sortSel
    }
  }




  override def toString() : String = {
    val builder = StringBuilder.newBuilder

    builder.append(s"buys : ${buys.size}")
    builder.append(System.getProperty("line.separator"))
    buys.foreach(b => {
      builder.append(s"quantity:${b.quantity} price:${b.price}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.append(s"sells : ${sels.size}")
    builder.append(System.getProperty("line.separator"))
    sels.foreach(s => {
      builder.append(s"quantity:${s.quantity} price:${s.price}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.toString()
  }

  override def getTopSel: Option[Order] = sels.lift(0)

  override def getLowSel: Option[Order] = sels.lastOption

  override def getTopBuy: Option[Order] = buys.lift(0)

  override def getLowBuy: Option[Order] = buys.lastOption
}
