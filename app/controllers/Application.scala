package controllers

import java.util.UUID

import actors.{GetResult, LicenseDetector}
import akka.actor.{ActorNotFound, Props}
import akka.util.Timeout
import akka.pattern.ask
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.Random


object Application extends Controller {

  def license(maybeId: Option[String]) = Action.async(parse.text) { request =>
    maybeId.fold {
      val id = UUID.randomUUID().toString
      val resultFuture = Akka.system.actorOf(Props(classOf[LicenseDetector], request.body), id).ask(GetResult)(Timeout(20.seconds))
      tryToGetResult(resultFuture, id).recover {
        // did not receive the result in time so redirect
        case e: TimeoutException => Found(routes.Application.licenseCheck(id).url)
      }
    } (existingJob)
  }

  def licenseCheck(id: String) = Action.async {
    existingJob(id)
  }

  private def existingJob(id: String): Future[Result] = {
    val resultFuture = Akka.system.actorSelection(s"user/$id").ask(GetResult)(Timeout(20.seconds))
    tryToGetResult(resultFuture, id).recover {
      // did not receive the result in time so redirect
      // workaround: Play's WS lib does not follow redirects to the same URL so a random number is appended to the query string
      case e: TimeoutException => Redirect(routes.Application.licenseCheck(id).url, Map("rand" -> Seq(Random.nextInt().toString)))
    }
  }

  private def tryToGetResult(resultFuture: Future[Any], id: String): Future[Result] = {
    resultFuture.mapTo[Option[String]].map { result =>
      // received the result
      Akka.system.actorSelection(s"user/$id").resolveOne(1.second).foreach(Akka.system.stop)
      result.fold(NotFound("License Not Detected"))(Ok(_))
    }
  }

}