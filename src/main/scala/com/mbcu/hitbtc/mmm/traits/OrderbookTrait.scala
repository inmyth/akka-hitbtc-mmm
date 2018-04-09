package com.mbcu.hitbtc.mmm.traits

import com.mbcu.hitbtc.mmm.models.response.Order

trait OrderbookTrait {



  def getTopSel : Order
  def getLowSel : Order
  def getTopBuy : Order
  def getLowBuy : Order
}
