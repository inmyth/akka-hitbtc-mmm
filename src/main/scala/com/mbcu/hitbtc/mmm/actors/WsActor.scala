package com.mbcu.hitbtc.mmm.actors

import java.util

import akka.actor.{Actor, ActorRef, Props}
import com.neovisionaries.ws.client.{WebSocketFactory, WebSocketListener, WebSocketState}
import akka.event.{EventBus, SubchannelClassification}
import com.mbcu.hitbtc.mmm.actors.WsActor._
import com.mbcu.hitbtc.mmm.utils.MyLogging
import com.neovisionaries.ws.client
import play.api.libs.json.{JsValue, Json}

object WsActor {
  def props(url : String): Props = Props(new WsActor(url))

  case class WsConnected()

  case class SendJs(jsValue: JsValue)

  case class SetParser(parser : ActorRef)

  case class WsGotText(text: String)
}

class WsActor(url: String) extends Actor with MyLogging{

private var ws : Option[com.neovisionaries.ws.client.WebSocket] = None
private var main: Option[ActorRef] = None

  override def receive: Receive = {

    case "start" =>
        main = Some(sender)
        val factory = new WebSocketFactory
        val websocket = factory.createSocket(url)
        websocket.addListener(ScalaWebSocketListener)
        websocket.connect()
        ws = Some(websocket)

    case SendJs(jsValue) =>
      val json : String = Json.stringify(jsValue)

      ws match {
        case Some(webSocket) => webSocket.sendText(json)
        case _ => println("WSActor#SendJs : No Websocket")
      }

  }

  object ScalaWebSocketListener extends WebSocketListener {

    override def onConnected(websocket: client.WebSocket, headers: util.Map[String, util.List[String]]): Unit = {
      info("Connected to " + url)
      main foreach (_ ! WsConnected)
    }

    def onTextMessage(websocket: com.neovisionaries.ws.client.WebSocket, data: String) : Unit = {
      main foreach (_ ! WsGotText(data))
    }

    override def onStateChanged(websocket: client.WebSocket, newState: WebSocketState): Unit = ???
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
