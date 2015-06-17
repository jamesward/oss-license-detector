package controllers

import java.util.UUID

import actors.{GetResult, LicenseDetector}
import akka.actor.{ActorNotFound, Props}
import akka.util.Timeout
import akka.pattern.ask
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.{TimeoutException, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {

  def license(maybeId: Option[String]) = Action.async(parse.text) { request =>

    val (resultFuture, id) = maybeId.fold {
      // no id so create a job
      val id = UUID.randomUUID().toString
      (Akka.system.actorOf(Props(classOf[LicenseDetector], request.body), id).ask(GetResult)(Timeout(20.seconds)), id)
    } { id =>
      (Akka.system.actorSelection(s"user/$id").ask(GetResult)(Timeout(20.seconds)), id)
    }

    resultFuture.mapTo[Option[String]].map { result =>
      // received the result
      Akka.system.actorSelection(s"user/$id").resolveOne(1.second).foreach(Akka.system.stop)
      result.fold(NotFound("License Not Detected"))(Ok(_))
    } recover {
      // did not receive the result in time so redirect
      case e: TimeoutException => Redirect(routes.Application.license(Some(id)))
    }

  }

}