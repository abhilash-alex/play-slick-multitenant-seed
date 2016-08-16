package dao.client

import dao.TenantDao
import models.ticket.{Ticket, TicketComment, TicketCommentTable, TicketTable}
import models.user._
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by abhilash on 12/7/16.
  */
class UserDao(implicit dbContext:ExecutionContext) extends TenantDao[UserTable, User]( User.userTable ){

}

class UserRoleDao(implicit dbContext:ExecutionContext) extends TenantDao[UserRoleTable, UserRole]( UserRole.userRoleTable ){

  // implement additional when needed
  protected def getRoleForUserQuery(userId:Long)={
    import RoleUtils.mappedTypeMapper
    for {
      user <- User.userTable
      userRoles <- query if user.id === userRoles.userId
    } yield (userRoles.role)
  }

  def getRoleForUser(userId:Long, databaseName:Option[String]):Future[Option[ClientRole]] = {
    getDb(databaseName).run(getRoleForUserQuery(userId).result.headOption)
  }

}