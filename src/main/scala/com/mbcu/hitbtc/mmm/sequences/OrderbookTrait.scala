package com.mbcu.hitbtc.mmm.sequences

import com.mbcu.hitbtc.mmm.models.response.Order

trait OrderbookTrait {

  def sort()    : Unit
  def sortBuy() : Unit
  def sortSel() : Unit

  def getTopSel : Option[Order]
  def getLowSel : Option[Order]
  def getTopBuy : Option[Order]
  def getLowBuy : Option[Order]
}
