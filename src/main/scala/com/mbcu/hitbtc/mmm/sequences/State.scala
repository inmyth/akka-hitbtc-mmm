package com.mbcu.hitbtc.mmm.sequences

import com.mbcu.hitbtc.mmm.models.internal.Config
import com.mbcu.hitbtc.mmm.models.response.Order

class State (var orderbooks : scala.collection.immutable.Map[String, Orderbook], val config : Config) {

  def fillOrderbook(ordersOption : Option[Seq[Order]]) : Unit = {
    ordersOption match {
      case Some(orders) => {
        orders
          .filter(order => orderbooks.contains(order.symbol))
          .foreach(order => {
            orderbooks.get(order.symbol) match {
              case Some(orderbook) => orderbook.add(order)
              case _ => println("MainActor#ActiveOrders : no orderbook")
            }
          })
      }
      case _ => println("MainActor#ActiveOrders : no orders")
    }
  }

  def logOrderbooks() : Unit = orderbooks foreach  {case (key, value) => println(key, value)}

  def initOrderbooks() : Unit = {
    config.bots foreach  (bot => {
      orderbooks = orderbooks +  (bot.pair -> new Orderbook(bot.pair, Seq.empty[Order], Seq.empty[Order]))
    })
  }

  def initActiveOrders() : Unit = {

  }

}
