package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, ActorRef, Props}
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model.SendEmailResult
import com.mbcu.hitbtc.mmm.actors.SesActor.{MailSent, SendError}
import com.mbcu.hitbtc.mmm.utils.MySES.{cli, tos}
import jp.co.bizreach.ses.SESClient
import jp.co.bizreach.ses.models.{Address, Content, Email}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Success, Try}


object  SesActor {
  def props(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]]): Props = Props(new SesActor(sesKey, sesSecret, emails))

  case class SendError(msg :String, shutdownCode : Option[Int])

  case class MailSent(t : Try[SendEmailResult], shutdownCode : Option[Int])

}


class SesActor(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]]) extends Actor {
  var tos : Seq[Address] = Seq.empty
  var cli : Option[SESClient] = None
  private implicit val region: Regions = Regions.US_EAST_1
  private var main : Option[ActorRef] = None

  override def receive: Receive = {

    case "start" =>
      main = Some(sender())
      sesKey match {
        case Some(key) => sesSecret match {
          case Some (secret) => emails match {
            case Some(list) =>
              cli = Some(SESClient(key, secret))
              this.tos ++= list map (Address(_))
          }
        }
      }


    case SendError(msg, shutdownCode) => sender() ! send("HitBTC Bot Error", msg, shutdownCode)

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
          //              sender() ! Some(f)
//          case _ => sender() ! None
        }
    }
  }

}
