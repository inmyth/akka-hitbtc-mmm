package com.mbcu.hitbtc.mmm.actors

import java.util

import akka.actor.{Actor, ActorRef, Props}
import com.neovisionaries.ws.client.{WebSocketFactory, WebSocketListener, WebSocketState}
import akka.event.{EventBus, SubchannelClassification}
import com.mbcu.hitbtc.mmm.actors.WsActor._
import com.mbcu.hitbtc.mmm.utils.MyLogging
import com.neovisionaries.ws.client
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}

object WsActor {
  def props(url : String): Props = Props(new WsActor(url))

  object WsConnected

  object WsDisconnected

  case class SendJs(jsValue: JsValue)

  case class SetParser(parser : ActorRef)

  case class WsGotText(text: String)

  case class WSError(msg :String, shutdownCode : Option[Int] = None)
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
      ws = Some(websocket)
      Try(websocket.connect()) match {
        case Success(succ) =>
        case Failure(fail) => main.foreach(_ ! WSError(s"WsActor#'start websocket.connect': $fail", Some(-1)))
      }

    case SendJs(jsValue) =>
      val json : String = Json.stringify(jsValue)

      ws match {
        case Some(webSocket) => webSocket.sendText(json)
        case _ => println("WSActor#SendJs : No Websocket")
      }

  }

  object ScalaWebSocketListener extends WebSocketListener {

    override def onConnected(websocket: client.WebSocket, headers: util.Map[String, util.List[String]]): Unit = {
      main foreach (_ ! WsConnected)
    }

    def onTextMessage(websocket: com.neovisionaries.ws.client.WebSocket, data: String) : Unit = {
      main foreach (_ ! WsGotText(data))
    }

    override def onStateChanged(websocket: client.WebSocket, newState: WebSocketState): Unit = {}
    override def handleCallbackError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: Throwable): Unit = {
      main foreach(_ ! WSError(s"WsActor#handleCallbackError: ${x$2.getMessage}", Some(-1)))
    }

    override def onBinaryFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onBinaryMessage(x$1: com.neovisionaries.ws.client.WebSocket, x$2: Array[Byte]): Unit = {}
    override def onCloseFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onConnectError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException): Unit = {
      main foreach(_ ! WSError(s"WsActor#onConnectError: ${x$2.getMessage}", Some(1)))
    }

    override def onContinuationFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onDisconnected(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame, x$3: com.neovisionaries.ws.client.WebSocketFrame, x$4: Boolean): Unit = {
      main foreach(_ ! WsDisconnected)
    }

    override def onError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException): Unit = {
      main foreach(_ ! WSError(s"WsActor#onError: ${x$2.getMessage}", Some(-1)))
    }

    override def onFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onFrameError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: com.neovisionaries.ws.client.WebSocketFrame): Unit = {
      main foreach(_ ! WSError(s"WsActor#onFrameError: ${x$2.getMessage}", Some(1)))
    }

    override def onFrameSent(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onFrameUnsent(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onMessageDecompressionError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: Array[Byte]): Unit = {
      main foreach(_ ! WSError(s"WsActor#onMessageDecompressionError: ${x$2.getMessage}", Some(1)))
    }

    override def onMessageError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: java.util.List[com.neovisionaries.ws.client.WebSocketFrame]): Unit = {
      main foreach(_ ! WSError(s"WsActor#onMessageError: ${x$2.getMessage}", Some(1)))
    }

    override def onPingFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onPongFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onSendError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: com.neovisionaries.ws.client.WebSocketFrame): Unit = {
      main foreach(_ ! WSError(s"WsActor#onSendError: ${x$2.getMessage}", Some(-1)))
    }

    override def onSendingFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onSendingHandshake(x$1: com.neovisionaries.ws.client.WebSocket, x$2: String, x$3: java.util.List[Array[String]]): Unit = {}
    override def onTextFrame(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketFrame): Unit = {}
    override def onTextMessageError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException, x$3: Array[Byte]): Unit = {
      main foreach(_ ! WSError(s"WsActor#onTextMessageError: ${x$2.getMessage}", Some(1)))
    }
    override def onThreadCreated(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.ThreadType, x$3: Thread): Unit = {}
    override def onThreadStarted(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.ThreadType, x$3: Thread): Unit = {}
    override def onThreadStopping(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.ThreadType, x$3: Thread): Unit = {}
    override def onUnexpectedError(x$1: com.neovisionaries.ws.client.WebSocket, x$2: com.neovisionaries.ws.client.WebSocketException): Unit = {
      main foreach(_ ! WSError(s"WsActor#onUnexpectedError: ${x$2.getMessage}", Some(-1)))
    }


  }



}
