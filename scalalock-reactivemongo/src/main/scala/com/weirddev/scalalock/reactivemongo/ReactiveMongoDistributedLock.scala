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

import com.weirddev.scalalock.api.DistributedLock
import com.weirddev.scalalock.model.LockRegistry
import reactivemongo.api.DefaultDB

import scala.concurrent.{ExecutionContext, Future}

/**
  * Date: 3/27/2019
  * @param database the persistence store for the lock collection. If using reactivemongo-play, inject - reactiveMongoApi: ReactiveMongoApi - than pass reactiveMongoApi.database
  * @param locksCollectionName the collection name where locks will be stored
  * @author Yaron Yamin
  */
class ReactiveMongoDistributedLock(database: Future[DefaultDB], locksCollectionName:String = LockRegistry.DefaultCollectionName)(implicit ec: ExecutionContext) extends DistributedLock(new ReactiveMongoDistributedLockRepository(database,locksCollectionName)(ec))(ec)