package com.mbcu.hitbtc.mmm.traits

import com.mbcu.hitbtc.mmm.models.response.Order

trait OrderbookTrait {



  def getTopSel : Option[Order]
  def getLowSel : Option[Order]
  def getTopBuy : Option[Order]
  def getLowBuy : Option[Order]
}
