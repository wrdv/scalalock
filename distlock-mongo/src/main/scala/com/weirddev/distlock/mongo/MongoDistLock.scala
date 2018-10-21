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

import com.weirddev.distlock.api.DistLock
import org.mongodb.scala.MongoDatabase

import scala.concurrent.ExecutionContext

/**
  * Date: 10/18/2018
  * @param mongoDb the persistence store for the lock collection
  * @param locksCollectionName the collection name where locks will be stored
  *
  * @author Yaron Yamin
  */
class MongoDistLock(mongoDb: MongoDatabase,locksCollectionName:String = "lock_registry")(implicit ec: ExecutionContext) extends DistLock(new MongoDistLockRepository(mongoDb,locksCollectionName)(ec))(ec)
