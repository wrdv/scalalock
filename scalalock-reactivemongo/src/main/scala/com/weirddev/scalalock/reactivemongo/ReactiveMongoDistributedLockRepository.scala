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

package com.weirddev.scalalock.reactivemongo

import com.weirddev.scalalock.api.AbstractLockRepository
import com.weirddev.scalalock.model.LockStates.LockState
import com.weirddev.scalalock.model.{LockRegistry, LockRegistryFieldName}
import reactivemongo.api.DB
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.CommandException

import java.net.InetAddress
import java.util.Date

import scala.concurrent.{ExecutionContext, Future}

/**
  * Date: 3/27/2019
  *
  * @author Yaron Yamin
  */
class ReactiveMongoDistributedLockRepository(database: Future[DB], locksCollectionName: String = LockRegistry.DefaultCollectionName)(implicit override val ec: ExecutionContext) extends AbstractLockRepository
{
  protected def lockRegistryColl: Future[BSONCollection] = database.map(_.collection(locksCollectionName))

  protected def findAndModify(resourceId: String, fromState: LockState, toState: LockState, secondsAgo: Long, optTaskId: Option[String]): Future[Option[LockRegistry]] = {
    implicit val reader: BSONDocumentReader[LockRegistry] = Macros.reader[LockRegistry]
    lockRegistryColl.flatMap { collection =>
      val selector = BSONDocument(
        LockRegistryFieldName.Id -> resourceId,
        "$or" -> BSONArray(
          BSONDocument(LockRegistryFieldName.State -> fromState.toString),
          BSONDocument(LockRegistryFieldName.RegisteredAt -> BSONDocument("$lt" -> BSONDateTime(new Date(System.currentTimeMillis - secondsAgo * 1000).getTime)))
        )
      )

      val modifier = BSONDocument(
        "$set" -> BSONDocument(
          LockRegistryFieldName.RegisteredAt -> BSONDateTime(new Date().getTime),
          LockRegistryFieldName.State -> toState.toString,
          LockRegistryFieldName.ByHost -> Option(InetAddress.getLocalHost.getHostName),
          LockRegistryFieldName.TaskId -> optTaskId
        )
      )

      val findAndModifyCommand = collection.findAndModify(selector, collection.updateModifier(modifier, fetchNewObject = true, upsert = true))

      findAndModifyCommand.map(_.result[LockRegistry])
    }
  }

  override protected def resolveMongoErrorCode(ex: Throwable): Option[Int] = {
    ex match {
      case CommandException.Code(errCode) => Some(errCode)
      case _ => None
    }
  }
}
