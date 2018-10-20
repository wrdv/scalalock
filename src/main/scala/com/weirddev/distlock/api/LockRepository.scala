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
  * Date: 19/10/2018
  *
  * @author Yaron Yamin
  */
trait LockRepository {
  def tryToLock[T](resourceId: String, expire: Duration): Future[Boolean]
  def releaseLock[T](resourceId: String, expire: Duration): Future[Boolean]
}
