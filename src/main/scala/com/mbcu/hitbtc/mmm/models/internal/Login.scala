package com.mbcu.hitbtc.mmm.models.internal

import play.api.libs.json.{JsValue, Json}

case class LoginParams(algo : String, pKey : String, nonce : String, signature : String)
object LoginParams {
  implicit val jsonFormat = Json.format[LoginParams]

  def from (credentials: Credentials) : LoginParams = {
    new LoginParams("HS256", credentials.pKey, credentials.nonce, credentials.signature)
  }
}


case class Login(params : LoginParams, method : String = "login")

object Login {
  implicit val jsonFormat = Json.format[Login]

  def from (config: Config) : Login = {
    new Login(LoginParams.from(config.credentials))
  }
}

