package com.mbcu.hitbtc.mmm.utils

import java.util.logging.Level
import java.util.logging.Logger

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.simpleemail.model.Destination
import jp.co.bizreach.ses.SESClient
import jp.co.bizreach.ses.models.{Address, Content, Email}

import scala.util.Try


object MySES extends MyLogging {
  private implicit val region = Regions.US_EAST_1
  var tos : Seq[Address] = Seq.empty
  var cli : Option[SESClient] = None


  def init(sesKey : Option[String], sesSecret : Option[String], emails : Option[Seq[String]] ) : Unit = {
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

  def send(title: String, body: String): Unit = {
    cli match {
      case Some(c) =>
        tos.headOption match {
          case Some(adr) =>
            val email = Email(
              Content(title),
              adr,
              Some(Content(body)),
              None,
              tos
            )
            cli foreach(_.send(email))
        }
    }
  }



//  case class Email(subject: Content,
//                   source: Address,
//                   bodyText: Option[Content] = None,
//                   bodyHtml: Option[Content] = None,
//                   to: Seq[Address] = Seq.empty,
//                   cc: Seq[Address] = Seq.empty,
//                   bcc: Seq[Address] = Seq.empty,
//                   replyTo: Seq[Address] = Seq.empty,
//                   returnPath: Option[String] = None,
//                   configurationSet: Option[String] = None,
//                   messageTags: Map[String, String] = Map.empty)
}