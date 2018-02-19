package com.mbcu.hitbtc.mmm.actors

import java.util

import akka.actor.{Actor, ActorRef, Props}
import com.neovisionaries.ws.client.{WebSocketFactory, WebSocketListener, WebSocketState}
import akka.event.{EventBus, SubchannelClassification}
import com.mbcu.hitbtc.mmm.actors.WsActor.{SendSync, WsConnected}
import com.neovisionaries.ws.client
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpVersion
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{HttpClientOptions, WebSocket}
import io.vertx.scala.core.http.HttpClientOptions
import play.api.libs.json.{JsValue, Json}

object WsActor {
  def props(url : String): Props = Props(new WsActor(url))

  case class WsConnected()

  case class SendSync(str : JsValue)


}

class WsActor(url: String) extends Actor {

private var ws : Option[com.neovisionaries.ws.client.WebSocket] = None
//  var ws: Option[WebSocket] = None
  var actorSender: Option[ActorRef] = None
//  var opts = HttpClientOptions()
//    .setDefaultHost("api.hitbtc.com")
//    .setDefaultPort(443)
//    .setProtocolVersion(HttpVersion.HTTP_2)
//    .setSsl(true)
//  val client = Some(Vertx.vertx().createHttpClient(opts))

  override def receive: Receive = {

    case "start" => {
        actorSender = Some(sender)
        val factory = new WebSocketFactory
        val websocket = factory.createSocket(url)
        websocket.addListener(ScalaWebSocketListener)
        websocket.connect()
        ws = Some(websocket)



      //  /api/2/ws
//      client.get.websocket("/api/2/ws", (websocket: WebSocket) => {
//
//        websocket.frameHandler(buffer => {
//          println(buffer.textData())
////         actorSender.map(_ ! buffer.textData())
////         actorSender.get ! RippledResponse(this.handlerId.get, buffer.textData())
//        })
//
//        ws = Some(websocket)
//        ws.get.writeFinalTextFrame("aaaa")
//      })


//      ws.get.writeFinalTextFrame("a")

    }



    case SendSync(jsValue) => {
      val str = Json.stringify(jsValue)
      ws match {

        case Some(webSocket) => webSocket.sendText(str)
        case _ => println("WS Actor SendSync No Websocket")
      }

    }




  }

  object ScalaWebSocketListener extends WebSocketListener {


    override def onStateChanged(websocket: client.WebSocket, newState: WebSocketState): Unit = ???

    override def onConnected(websocket: client.WebSocket, headers: util.Map[String, util.List[String]]): Unit = {
      println(s"connected to ", url)
      actorSender map (_ ! WsConnected)
    }

    def onTextMessage(websocket: com.neovisionaries.ws.client.WebSocket, data: String) = {
      println(data)
      actorSender.map(_ ! data)
    }

    def handleCallbackError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: Throwable): Unit = ???
    def onBinaryFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onBinaryMessage(x$1: com.neovisionaries.ws.client.WebSocket, x$2: Array[Byte]): Unit = ???
    def onCloseFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onConnectError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException): Unit = ???
    def onContinuationFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onDisconnected(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame, x$3: com.neovisionaries.ws.client.WebSocketFrame, x$4: Boolean): Unit = ???
    def onError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException): Unit = ???
    def onFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onFrameError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onFrameSent(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onFrameUnsent(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onMessageDecompressionError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: Array[Byte]): Unit = ???
    def onMessageError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: java.util.List[com.neovisionaries.ws.client.WebSocketFrame]): Unit = ???
    def onPingFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onPongFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onSendError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onSendingFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onSendingHandshake(x$1: com.neovisionaries.ws.client.WebSocket, x$2: String, x$3: java.util.List[Array[String]]): Unit = ???
    def onTextFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = ???
    def onTextMessageError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: Array[Byte]): Unit = ???
    def onThreadCreated(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.ThreadType, x$3: Thread): Unit = ???
    def onThreadStarted(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.ThreadType, x$3: Thread): Unit = ???
    def onThreadStopping(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.ThreadType, x$3: Thread): Unit = ???
    def onUnexpectedError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException): Unit = ???


  }



}
