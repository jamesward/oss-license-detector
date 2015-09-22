package controllers

import play.api.Play
import play.api.cache.Cache
import play.api.libs.Codecs
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.LicenseUtil

import scala.concurrent.{Promise, Future, TimeoutException}
import scala.concurrent.duration._

import scala.util.Random


object Application extends Controller {

  def license = Action.async(parse.text) { request =>
    val id = Codecs.sha1(request.body)

    val maybeResultFuture = Cache.getAs[Option[String]](id).fold {
      // cache miss

      TimeoutFuture(25.seconds) {
        Future {
          val maybeLicense = LicenseUtil(Play.current).detect(request.body)
          Cache.set(id, maybeLicense)
          maybeLicense
        }
      }

    } (Future.successful)

    tryToGetResult(maybeResultFuture, id)
  }

  def licenseCheck(id: String) = Action.async {
    val maybeResultFuture = Cache.getAs[Option[String]](id).fold {
      // cache miss, try again later
      TimeoutFuture(25.seconds) {
        Cache.getAs[Option[String]](id).fold(Future.failed[Option[String]](new TimeoutException))(Future.successful)
      }
    } (Future.successful)

    tryToGetResult(maybeResultFuture, id)
  }

  private def tryToGetResult(resultFuture: Future[Option[String]], id: String): Future[Result] = {
    resultFuture.map { result =>
      result.fold(NotFound("License Not Detected"))(Ok(_))
    } recover {
      case e: TimeoutException =>
        Redirect(routes.Application.licenseCheck(id).url, Map("rand" -> Seq(Random.nextInt().toString)))
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