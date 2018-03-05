package com.mbcu.hitbtc.mmm.actors

import akka.actor.{Actor, Props}
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model.SendEmailResult
import com.mbcu.hitbtc.mmm.actors.SesActor.SendError
import com.mbcu.hitbtc.mmm.utils.MySES.{cli, tos}
import jp.co.bizreach.ses.SESClient
import jp.co.bizreach.ses.models.{Address, Content, Email}

import scala.concurrent.Future


object  SesActor {
  def props(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]]): Props = Props(new SesActor(sesKey, sesSecret, emails))

  case class SendError(msg :String)

}


class SesActor(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]]) extends Actor {
  var tos : Seq[Address] = Seq.empty
  var cli : Option[SESClient] = None
  private implicit val region = Regions.US_EAST_1

  override def receive: Receive = {

    case "start" =>   def init(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]] ) : Unit = {
      sesKey match {
        case Some(key) => sesSecret match {
          case Some (secret) => emails match {
            case Some(list) =>
              cli = Some(SESClient(key, secret))
              this.tos ++= list map (Address(_))
          }
        }
      }
    }

    case SendError(msg) => sender() ! send("HitBTC Bot Error", msg)

  }

  def send(title: String, body: String): Future[SendEmailResult] = {
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
            case Some(c) => c send email
            case _ => Future.failed(new Exception("SesActor#send no client"))
          }
        case _ => Future.failed(new Exception("SesActor#send emails empty"))
      }
  }

}
