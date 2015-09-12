package controllers

import actors.{GetResult, LicenseDetector}
import akka.actor.Props
import akka.util.Timeout
import akka.pattern.ask
import play.api.cache.Cache
import play.api.libs.Codecs
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.{Promise, Future, TimeoutException}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.{Try, Random}


object Application extends Controller {

  def license(maybeId: Option[String]) = Action.async(parse.text) { request =>
    val id = maybeId.getOrElse(Codecs.sha1(request.body))

    Cache.getAs[String](id).fold {
      // cache miss
      val actorSystem = Akka.system

      // we need to see if a job already exists but we can't use actorSystem.actorSelection(s"user/$id").resolveOne
      // because it sends a message to the actor and in our case the actor is likely blocked
      // so instead lets just try to create the actor and if it fails due to InvalidActorNameException
      // then we know there is already one
      val maybeActor = Try {
        actorSystem.actorOf(Props(classOf[LicenseDetector], request.body), id)
      }.toOption

      // check for an existing job
      maybeActor.fold(existingJob(id)) { actor =>
        // create a new job
        val resultFuture = actor.ask(GetResult)(Timeout(5.minutes))

        // shut down the actor 5 seconds after it is finished processing
        resultFuture.foreach { result =>
          Cache.set(id, result)
          actorSystem.stop(actor)
        }

        TimeoutFuture(20.seconds)(resultFuture).flatMap { result =>
          tryToGetResult(resultFuture, id)
        } recover {
          case e: TimeoutException =>
            Found(routes.Application.licenseCheck(id).url)
        }
      }
    } { license =>
      // cache hit
      Future.successful(Ok(license))
    }
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
      result.fold(NotFound("License Not Detected"))(Ok(_))
    }
  }

  object TimeoutFuture {
    def apply[A](timeout: FiniteDuration)(future: Future[A]): Future[A] = {

      val promise = Promise[A]()

      Akka.system.scheduler.scheduleOnce(timeout) {
        promise.tryFailure(new TimeoutException)
      }

      promise.completeWith(future)

      promise.future
    }
  }

}