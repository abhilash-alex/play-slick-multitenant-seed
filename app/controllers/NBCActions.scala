package controllers

import com.typesafe.config.Config
import models.web.{ClientToken, NBCAuthenticatedRequest}
import org.asynchttpclient.netty.handler.intercept.Unauthorized401Interceptor
import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.mvc.Results.Unauthorized
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import pdi.jwt._


/**
  * Created by abhilash on 7/7/16.
  */

object AuthenticatedAction extends ActionBuilder[NBCAuthenticatedRequest] {
  override def invokeBlock[A](request: Request[A], block: (NBCAuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    request.jwtSession.getAs[ClientToken]("payload") match { //todo get header for token from conf
      case Some(clientToken) => block(new NBCAuthenticatedRequest[A](clientToken, request)).map( _.refreshJwtSession(request) )
      case _ => Future.successful(Unauthorized)
    }
  }
}

//todo admin action
