package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model.SendEmailResult
import com.mbcu.hitbtc.mmm.actors.SesActor.{CacheMessages, MailSent, MailTimer}
import jp.co.bizreach.ses.SESClient
import jp.co.bizreach.ses.models.{Address, Content, Email}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContextExecutor
import scala.util.Try


object  SesActor {
  def props(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]]): Props = Props(new SesActor(sesKey, sesSecret, emails))

  case class CacheMessages(msg :String, shutdownCode : Option[Int])

  object MailTimer

  case class MailSent(t : Try[SendEmailResult], shutdownCode : Option[Int])

}


class SesActor(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]]) extends Actor {
  var tos : Seq[Address] = Seq.empty
  var cli : Option[SESClient] = None
  private implicit val region: Regions = Regions.US_EAST_1
  private var main : Option[ActorRef] = None
  private var isCaching : Boolean = false
  private val cacheMsg = new ListBuffer[String]()
  private var cacheShutdownCode : Option[Int] = None

  override def receive: Receive = {

    case "start" =>
      main = Some(sender())
      sesKey match {
        case Some(key) => sesSecret match {
          case Some (secret) => emails match {
            case Some(list) =>
              cli = Some(SESClient(key, secret))
              this.tos ++= list map (Address(_))
            case _ =>
          }
            case _ =>
        }
        case _ =>
      }


    case CacheMessages(msg, shutdownCode) =>
      if (!isCaching){
        isCaching = true
        main foreach(_ ! MailTimer)
      }
      cacheMsg += msg

      cacheShutdownCode = (shutdownCode, cacheShutdownCode) match {
        case (Some(code), None) => Some(code)
        case (_, Some(-1)) => Some(-1)
        case (Some(1), Some(1)) => Some(1)
        case (None ,Some(1)) => Some(1)
        case (Some(-1), Some(1)) => Some(-1)
        case _ => None // (None, None)
      }

    case "execute send" =>
      send("HitBTC Bot Error", cacheMsg.mkString("\n\n"), cacheShutdownCode)
      cacheMsg.clear()
      cacheShutdownCode = None
      isCaching = false
  }

  def send(title: String, body: String, shutdownCode : Option[Int] = None): Unit = {
    implicit val executor: ExecutionContextExecutor =  scala.concurrent.ExecutionContext.global
    tos.headOption match {
      case Some(adr) =>
        val email = Email(
          Content(title),
          adr,
          Some(Content(body)),
          None,
          tos
        )
        cli match {
          case Some(c) =>
            val f = c send email
            f.onComplete(t => main foreach(_ ! MailSent(t, shutdownCode)))
          case _ => println("SesActor#send no client")

        }
      case _ =>
    }
  }

}
