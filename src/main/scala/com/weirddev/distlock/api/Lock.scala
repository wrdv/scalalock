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
package com.weirddev.distlock.api

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * Date: 10/18/2018
 *
  * @author Yaron Yamin
  */
trait Lock {

  /**
    * Locks concurrent task execution in a cluster for the same resource.
    * this lock will not wait in resource is already locked and will return Future[None] in such case
    *
    * @param resourceId A name which identifies the locked resource. Lock can be used globally to lock different resources/tasks
    * @param expire Auto expiration period. it's recommended to always set some reasonable value.
    *               useful when task is stuck for longer than anticipated or when there's a system crash.
    *               since the lock is persisted, it will not unlock on restart in jvm crash scenario unless there an expiration period set.
    * @param synchronizedTask The synchronized function to invoke if locking was successful.
    * @return Returns Future[None] in case resouce is locked.
    *         Returns Future[Some[Future[T] ] ]  in case lock was successful, where Future[T] is the computed result of the concurrent task
    */
  def lock[T](resourceId: String, expire: Duration = Duration.Inf)(synchronizedTask: => T): Future[Option[T]]

  //todo overload with a promise and document

}
