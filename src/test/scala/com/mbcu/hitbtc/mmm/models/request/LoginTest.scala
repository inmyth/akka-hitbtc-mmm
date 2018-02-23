package com.mbcu.hitbtc.mmm.models.request

import org.scalatest.FunSuite
import play.api.libs.json.{JsValue, Json}

class LoginTest extends FunSuite{

    test("RequestLogin.scala Serialize") {
      val str: String =
        """
          |{
          | "method": "login",
          | "params": {
          |    "algo": "HS256",
          |    "pKey": "fd5f0aa793405f356beba728170e7a13",
          |    "nonce": "abcabc",
          |    "signature": "signsignsign"
          | }
          |}
        """.stripMargin

      val str2Json: JsValue = Json.parse(str)
      assert((str2Json \ "method").as[String] === "login")
      assert((str2Json \ "params" \ "nonce").as[String] === "abcabc")
      assert((str2Json \ "params" \ "signature").as[String] === "signsignsign")

    }

  test("RequestLogin.scala To Object" ) {
    val params = new LoginParams("HS256", "fd5f0aa793405f356beba728170e7a13", "abcabc", "signsignsign")
    val login = new Login(params)
    val obj2Json: JsValue = Json.toJson(login)
    val json2Obj: Login = obj2Json.as[Login]

    assert(json2Obj.method === "login")
    assert(json2Obj.params.algo === "HS256")
    assert(json2Obj.params.pKey === "fd5f0aa793405f356beba728170e7a13")


  }

  test("RequestLogin.scala Deserialize" ) {
    val params = new LoginParams("HS256", "fd5f0aa793405f356beba728170e7a13", "abcabc", "signsignsign")
    val login = new Login(params)
    val obj2Json: JsValue = Json.toJson(login)
    val json2Str: String = Json.stringify(obj2Json)

    assert(json2Str.contains("login"))
    assert(json2Str.contains("HS256"))
    assert(json2Str.contains("fd5f0aa793405f356beba728170e7a13"))

  }

}
