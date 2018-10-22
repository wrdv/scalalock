/*
 * Copyright 2018 WeirdDev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weirddev.distlock.mongo

import java.net.InetAddress
import java.util.Date

import com.mongodb.client.model.ReturnDocument
import com.mongodb.{MongoCommandException, WriteConcern}
import com.weirddev.distlock.api.LockRepository
import com.weirddev.distlock.mongo.LockStates.LockState
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, Updates}
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Date: 19/10/2018
  * @param mongoDb the persistence store for the lock collection
  * @param locksCollectionName the collection name where locks will be stored
  *
  * @author Yaron Yamin
  */
protected class MongoDistLockRepository(mongoDb: MongoDatabase,locksCollectionName:String = "lock_registry")(implicit ec: ExecutionContext) extends LockRepository {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  val DuplicateKeyErrorCode = 11000

  object LockRegistryFieldName{
    val Id = "_id"
    val RegisteredAt = "registeredAt"
    val State = "state"
    val ByHost = "byHost"
    val TaskId = "taskId"
  }

  protected val LockRegistryColl: MongoCollection[LockRegistry] = mongoDb.getCollection[LockRegistry](locksCollectionName).withCodecRegistry(
    fromRegistries(fromProviders(classOf[LockRegistry]), DEFAULT_CODEC_REGISTRY)).withWriteConcern(WriteConcern.ACKNOWLEDGED
  )

  override def releaseLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean] = {
    findAndModify(resourceId, LockStates.LOCKED, LockStates.OPEN, expire.toSeconds, optTaskId) transform transformDaoResponse
  }

  override def tryToLock[T](resourceId: String, expire: Duration, optTaskId: Option[String]): Future[Boolean] = {
    findAndModify(resourceId, LockStates.OPEN, LockStates.LOCKED, expire.toSeconds, optTaskId) transform transformDaoResponse
  }

  protected def transformDaoResponse[T]: Try[T]=>Try[Boolean] = {
    case Success(_) =>
      Success(true)
    case Failure(ex: MongoCommandException) if ex.getErrorCode == DuplicateKeyErrorCode  =>
      /*DuplicateKey is the expected behaviour when document state is locked ( and not expired yet) since findAndModify was executed with upsert = true( to simplify handling the edge case where lock_registry doc not inserted yet) */
      Success(false)
    case Failure(ex) =>
      Failure(ex)
  }

  protected def findAndModify(resourceId:String, fromState: LockState, toState: LockState, secondsAgo:Long, optTaskId: Option[String]): Future[Option[LockRegistry]]  = {
    log.info(s"trying to find a lock registry document by _id=$resourceId in state=$fromState or with registeredAt < (sysdate - $secondsAgo seconds ago )")
    LockRegistryColl.findOneAndUpdate(
      Filters.and(
        Filters.eq(LockRegistryFieldName.Id,resourceId),
        Filters.or(
          Filters.eq(LockRegistryFieldName.State, fromState.toString),
          Filters.lt(LockRegistryFieldName.RegisteredAt, new Date(System.currentTimeMillis - secondsAgo * 1000))
        )
      ),
      Updates.combine(
        Updates.currentDate(LockRegistryFieldName.RegisteredAt),
        Updates.set(LockRegistryFieldName.State,toState.toString),
        Updates.set(LockRegistryFieldName.ByHost,InetAddress.getLocalHost.getHostName),
        Updates.set(LockRegistryFieldName.TaskId, optTaskId.orNull)
      ),
      FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    )
      .andThen(handleResponse(s"find and modify lock registry")).headOption()
  }

  protected def handleResponse[T](tracedOperation:String,logException:Boolean = true, traceResponse:Boolean=true): PartialFunction[Try[T],Boolean]  = {
    case Failure(ex) if ex.isInstanceOf[MongoCommandException] && ex.asInstanceOf[MongoCommandException].getErrorCode == DuplicateKeyErrorCode =>
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
