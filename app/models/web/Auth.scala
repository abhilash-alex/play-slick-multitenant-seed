package models.web

import models.user.ClientRole
import play.api.mvc._
import play.api.libs.json.Json

/**
  * Created by abhilash on 7/7/16.
  */

case class NBCAuthenticatedRequest[A](clientToken: ClientToken, request:Request[A]) extends WrappedRequest[A](request)
case class ClientToken(userId:Long, clientCode:String)
object ClientToken{
  implicit val clientTokenReads = Json.reads[ClientToken]
}
