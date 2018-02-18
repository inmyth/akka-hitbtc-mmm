package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.Done
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._

import scala.concurrent.Future
import akka.NotUsed
import akka.actor.AbstractActor.Receive
import akka.dispatch.ExecutionContexts.global
import akka.stream.OverflowStrategy
import play.api.libs.json.{JsDefined, JsObject, JsValue, Json}

object WsStreamActor {

  def props(url : String): Props = Props(new WsStreamActor(url))

  case class WsStreamConnected()
}

class WsStreamActor(url :String) extends Actor{

  var actorSender: Option[ActorRef] = None
  private var ws : Option[ActorRef] = None
  implicit val ec = global


  override def receive: Receive = {

    case "start" => {
      actorSender = Some(sender())

      implicit val materializer = ActorMaterializer()
      val req = WebSocketRequest(url)

      val messageSource: Source[Message, ActorRef] = Source.actorRef[TextMessage.Strict](bufferSize = 10, OverflowStrategy.fail)

      val webSocketFlow = Http(context.system).webSocketClientFlow(req)

      val messageToJsValueFlow = Flow[Message].collect {
        case message: TextMessage.Strict => message.textStream.map(Json.parse(_)) collect {
//          case jsValue if (isValidRequest(jsValue)) => jsValue
          case jsValue  => jsValue
//          case _ => Json.obj("command" -> "")
        } recover {
          case e: Exception =>
            println(e)
            Json.obj("command" -> "")
        }
      }
/*
Streams always start flowing from a Source[Out,M1] then can continue through Flow[In,Out,M2] elements or more advanced graph elements to finally be consumed by a Sink[In,M3]
(ignore the type parameters M1, M2 and M3 for now, they are not relevant to the types of the elements produced/consumed by these classes – they are “materialized types”, which we’ll talk about below)
 */
      val JsValueToMessageFlow = Flow[Source[JsValue, _]] map { jsValueStream =>
        TextMessage.Streamed(jsValueStream.map(msg => {
          Json.stringify(msg)
        }))
      }
      val messageSink: Sink[Message, NotUsed] =
        Flow[Message]
          .map(message => println(s"Received text message: [$message]"))
          .map(m => println("go to db"))
          .to(Sink.ignore)

      //    val aFlow: Flow[Message, NotUsed] = Flow[Message]
      //      .map(message -> println())

      val nothing : Flow[Message, Message, NotUsed] = Flow[Message] map {
        m => m
      }

      val toJs : Flow[Message, JsValue, NotUsed] = Flow[Message] map {
        m => Json.parse(m.toString)
      }

      val apiKeyFlow = Flow[Source[JsValue, _]] map { jvs =>
        jvs map { jsValue =>
        {
          if ((jsValue \ "secret").isInstanceOf[JsDefined] && (jsValue \ "tx_json").isInstanceOf[JsDefined]) {
            val updatedJsValue = jsValue.as[JsObject] + ("secret" -> Json.toJson("aaa"))
            updatedJsValue
          } else {
            jsValue
          }
        }
        }
      }

      val ((ws, upgradeResponse), closed) =
        messageSource
          .viaMat(webSocketFlow)(Keep.both)
          .viaMat(nothing)(Keep.left)
          .viaMat(messageToJsValueFlow)(Keep.left)
          .viaMat(apiKeyFlow)(Keep.left)
          .viaMat(JsValueToMessageFlow)(Keep.left)
          .toMat(messageSink)(Keep.both)
//            .viaMat(webSocketFlow)(Keep.both)
          .run()


//      def sinkFlow = Http().webSocketClientFlow(WebSocketRequest(url))


      val connected = upgradeResponse.flatMap { upgradeResponse: WebSocketUpgradeResponse =>
        if (upgradeResponse.response.status == StatusCodes.SwitchingProtocols) {
//          println("WsStreamActor connected")
          Future.successful(Done)
        } else {
          throw new RuntimeException(s"Connection failed: ${upgradeResponse.response.status}")
        }
      }

      this.ws = Some(ws)
    }

  }

}
