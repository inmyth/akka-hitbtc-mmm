package com.mbcu.hitbtc.mmm.models.request

import com.mbcu.hitbtc.mmm.models.internal.{Config, Credentials}
import play.api.libs.json.{JsPath, Json, OFormat, Reads}

case class LoginParams(algo : String, pKey : String, nonce : String, signature : String)
object LoginParams {
  implicit val jsonFormat: OFormat[LoginParams] = Json.format[LoginParams]

  def from (credentials: Credentials) : LoginParams = {
    new LoginParams("HS256", credentials.pKey, credentials.nonce, credentials.signature)
  }

}


case class Login(params : LoginParams, method : String = "login", id : String = "login")

object Login {
  implicit val jsonFormat: OFormat[Login] = Json.format[Login]

  def from (config: Config) : Login = {
    new Login(LoginParams.from(config.credentials))
  }

}

