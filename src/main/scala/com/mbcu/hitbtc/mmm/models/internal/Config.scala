package com.mbcu.hitbtc.mmm.models.internal

import play.api.libs.json.Json


case class Credentials (api : String, secret : String)
object Credentials {
  implicit val jsonFormat = Json.format[Credentials]

}

case class Bot (
pair              : String,
startMiddlePrice  : String,
gridSpace         : String,
buyGridLevels     : Int,
sellGridLevels    : Int,
buyOrderQuantity  : String,
sellOrderQuantity : String,
strategy          : String
)
object Bot {
  implicit val jsonFormat = Json.format[Bot]
}

case class Config (credentials: Credentials, bots : List[Bot])
object Config {
  implicit val jsonFormat = Json.format[Config]

}