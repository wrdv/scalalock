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
package com.weirddev.scalalock.mongo

import java.net.InetAddress
import java.util.Date

import com.mongodb.client.model.ReturnDocument
import com.mongodb.{MongoCommandException, WriteConcern}
import com.weirddev.scalalock.api.AbstractLockRepository
import com.weirddev.scalalock.model.LockStates.LockState
import com.weirddev.scalalock.model.{LockRegistry, LockRegistryFieldName}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, Updates}
import org.mongodb.scala.{MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Date: 19/10/2018
  * @param mongoDb the persistence store for the lock collection
  * @param locksCollectionName the collection name where locks will be stored
  *
  * @author Yaron Yamin
  */
protected class MongoDistributedLockRepository(mongoDb: MongoDatabase, locksCollectionName:String = LockRegistry.DefaultCollectionName)(implicit override val ec: ExecutionContext) extends AbstractLockRepository {

  protected val LockRegistryColl: MongoCollection[LockRegistry] = mongoDb.getCollection[LockRegistry](locksCollectionName).withCodecRegistry(
    fromRegistries(fromProviders(classOf[LockRegistry]), DEFAULT_CODEC_REGISTRY)).withWriteConcern(WriteConcern.ACKNOWLEDGED
  )

  protected def findAndModify(resourceId: String, fromState: LockState, toState: LockState, secondsAgo: Long, optTaskId: Option[String]): Future[Option[LockRegistry]] = {
    LockRegistryColl.findOneAndUpdate(
      Filters.and(
        Filters.eq(LockRegistryFieldName.Id, resourceId),
        Filters.or(
          Filters.eq(LockRegistryFieldName.State, fromState.toString),
          Filters.lt(LockRegistryFieldName.RegisteredAt, new Date(System.currentTimeMillis - secondsAgo * 1000))
        )
      ),
      Updates.combine(
        Updates.currentDate(LockRegistryFieldName.RegisteredAt),
        Updates.set(LockRegistryFieldName.State, toState.toString),
        Updates.set(LockRegistryFieldName.ByHost, InetAddress.getLocalHost.getHostName),
        Updates.set(LockRegistryFieldName.TaskId, optTaskId.orNull)
      ),
      FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFutureOption()
  }

  protected def resolveMongoErrorCode(ex:Throwable): Option[Int] = ex match {
    case exception: MongoCommandException =>
      Some(exception.getErrorCode)
    case _ =>
      None
  }
}
