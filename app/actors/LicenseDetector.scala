package actors

import akka.actor.Actor
import play.api.Play
import utils.LicenseUtil

class LicenseDetector(licenseText: String) extends Actor {

  lazy val detectedLicense = LicenseUtil(Play.current).detect(licenseText)

  override def receive = {
    case GetResult => sender() ! detectedLicense
  }
}

case object GetResult
