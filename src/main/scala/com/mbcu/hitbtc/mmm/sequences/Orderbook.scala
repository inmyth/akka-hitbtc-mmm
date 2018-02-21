package com.mbcu.hitbtc.mmm.sequences

import com.mbcu.hitbtc.mmm.models.response.Order

class Orderbook (var pair: String, var sels : List[Order], var buys : List[Order]) extends OrderbookTrait {

  override def sort: Unit = ???

  override def sortBuy: Unit = ???

  override def sortSel: Unit = ???
}
