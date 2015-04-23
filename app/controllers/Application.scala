package controllers

import play.api.Play
import play.api.mvc._
import utils.LicenseUtil


object Application extends Controller {

  lazy val licenseUtil = LicenseUtil(Play.current)

  def license = Action(parse.text) { request =>
    licenseUtil.detect(request.body).fold(NotFound("License Not Detected"))(Ok(_))
  }

}