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

import java.net.InetAddress
import java.util.Date

import com.weirddev.scalalock.api.AbstractLockRepository
import com.weirddev.scalalock.model.LockStates.LockState
import com.weirddev.scalalock.model.{LockRegistry, LockRegistryFieldName}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.bson.DefaultBSONCommandError
import reactivemongo.bson
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, Macros}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Date: 3/27/2019
  *
  * @author Yaron Yamin
  */
class ReactiveMongoDistributedLockRepository(database: Future[DefaultDB], locksCollectionName: String = LockRegistry.DefaultCollectionName)(implicit override val ec: ExecutionContext) extends AbstractLockRepository
{
  protected def lockRegistryColl: Future[BSONCollection] = database.map(_.collection(locksCollectionName))

  protected def findAndModify(resourceId: String, fromState: LockState, toState: LockState, secondsAgo: Long, optTaskId: Option[String]): Future[Option[LockRegistry]] = {

    implicit val reader: BSONDocumentReader[LockRegistry] = Macros.reader[LockRegistry]
    lockRegistryColl flatMap { collection =>
      import collection.BatchCommands.FindAndModifyCommand.FindAndModifyResult
      val result: Future[FindAndModifyResult] = collection.findAndUpdate(
        BSONDocument(
          LockRegistryFieldName.Id -> resourceId,
          f"$$or" -> bson.BSONArray(
            BSONDocument(LockRegistryFieldName.State -> fromState.toString),
            BSONDocument(LockRegistryFieldName.RegisteredAt -> BSONDocument(f"$$lt" -> new Date(System.currentTimeMillis - secondsAgo * 1000))),
          )
        ),
        BSONDocument(f"$$set" -> BSONDocument(
          LockRegistryFieldName.RegisteredAt -> new Date(),
          LockRegistryFieldName.State -> toState.toString,
          LockRegistryFieldName.ByHost -> Option(InetAddress.getLocalHost.getHostName),
          LockRegistryFieldName.TaskId -> optTaskId,
        )),
        fetchNewObject = true, upsert = true)
      result.map(_.result[LockRegistry])
    }
  }

  override protected def resolveMongoErrorCode(ex: Throwable): Option[Int] = {
    ex match {
      case DefaultBSONCommandError(Some(errCode), _,_) =>
        Some(errCode)
      case _ =>
        None
    }
  }
}
