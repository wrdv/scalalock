package com.weirddev.distlock.mongo

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Date: 19/10/2018
  *
  * @author Yaron Yamin
  */
trait LockRepository {
  def tryToLock[T](resourceId: String, expire: Duration): Future[Boolean]
  def releaseLock[T](resourceId: String, expire: Duration): Future[Boolean]
}
