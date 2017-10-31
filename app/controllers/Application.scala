package controllers

import javax.inject.Inject

import play.api.Play
import play.api.cache.SyncCacheApi
import play.api.libs.concurrent.Futures
import play.api.libs.Codecs
import play.api.libs.concurrent.Akka
import play.api.mvc._
import utils.LicenseUtil

import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.concurrent.duration._
import scala.util.Random


class Application @Inject() (licenseUtil: LicenseUtil, cache: SyncCacheApi, futures: Futures) (implicit ec: ExecutionContext) extends InjectedController {

  def license = Action.async(parse.text) { request =>
    val id = Codecs.sha1(request.body)

    val maybeResultFuture = cache.get[Option[String]](id).fold {
      // cache miss
      futures.timeout(25.seconds) {
        Future {
          val maybeLicense = licenseUtil.detect(request.body)
          cache.set(id, maybeLicense)
          maybeLicense
        }
      }

    } (Future.successful)

    tryToGetResult(maybeResultFuture, id)
  }

  def licenseCheck(id: String) = Action.async {
    val maybeResultFuture = cache.get[Option[String]](id).fold {
      // cache miss, try again later
      futures.timeout(25.seconds) {
        cache.get[Option[String]](id).fold(Future.failed[Option[String]](new TimeoutException))(Future.successful)
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

}
