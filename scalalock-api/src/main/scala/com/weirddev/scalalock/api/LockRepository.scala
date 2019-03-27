/*
 *  Copyright 2019 WeirdDev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.weirddev.scalalock.api

import com.weirddev.scalalock.model.LockStates.LockState
import com.weirddev.scalalock.model.{LockRegistry, LockStates}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Date: 19/10/2018
  *
  * @author Yaron Yamin
  */
trait LockRepository {
  def tryToLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean]
  def releaseLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean]
}

abstract class AbstractLockRepository(implicit val ec: ExecutionContext) extends LockRepository{

  val DuplicateKeyErrorCode = 11000
  private val log: Logger = LoggerFactory.getLogger(getClass)

  override def releaseLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean] = {
    safeFindAndModify(resourceId, LockStates.LOCKED, LockStates.OPEN, expire.toSeconds, optTaskId) transform transformDbResponse
  }

  override def tryToLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean] = {
    safeFindAndModify(resourceId, LockStates.OPEN, LockStates.LOCKED, expire.toSeconds, optTaskId) transform transformDbResponse
  }

  protected def resolveMongoErrorCode(ex:Throwable): Option[Int]
  protected def findAndModify(resourceId: String, fromState: LockState, toState: LockState, secondsAgo: Long, optTaskId: Option[String]): Future[Option[LockRegistry]]

  protected def safeFindAndModify(resourceId:String, fromState: LockState, toState: LockState, secondsAgo:Long, optTaskId: Option[String]): Future[Option[LockRegistry]]  = {
    log.info(s"trying to find a lock registry document by _id=$resourceId in state=$fromState or with registeredAt < (sysdate - $secondsAgo seconds ago )")
    findAndModify(resourceId, fromState, toState, secondsAgo, optTaskId)
      .andThen(handleResponse(s"find and modify lock registry"))
  }

  protected def transformDbResponse[T]: Try[T]=>Try[Boolean] = {
    case Success(_) =>
      Success(true)
    case Failure(ex) if resolveMongoErrorCode(ex).contains(DuplicateKeyErrorCode ) =>
      /*DuplicateKey is the expected behaviour when document state is locked ( and not expired yet) since findAndModify was executed with upsert = true( to simplify handling the edge case where lock_registry doc not inserted yet) */
      Success(false)
    case Failure(ex) =>
      Failure(ex)
  }

  protected def handleResponse[T](tracedOperation:String,logException:Boolean = true, traceResponse:Boolean=true): PartialFunction[Try[T],Boolean]  = {
    case Failure(ex) if resolveMongoErrorCode(ex).contains(DuplicateKeyErrorCode) =>
      log.info("ignoring mongo duplicate key error for "+tracedOperation)
      false
    case Failure(ex) =>
      val errMsg = tracedOperation + " failed"
      if(logException) {
        log.error(errMsg, ex)
      }
      false
    case Success(ret) =>
      if(traceResponse) {
        log.info(tracedOperation + " returned: " + ret)
      }
      true
  }

}