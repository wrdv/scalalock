package com.weirddev.distlock.api

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Date: 19/10/2018
  *
  * @author Yaron Yamin
  */
trait LockRepository {
  def tryToLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean]
  def releaseLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean]
}
