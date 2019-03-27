package com.weirddev.scalalock.api

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
    * @param taskId Task name of auditing. sometimes the same resource is accessed by different task types ( i.e. publisher/consumer)
    * @param releaseLockWhenDone When true, lock will be release on task completion. Setting to false is useful when trying to mandate the task execution interval globally.
    *                         In such case the lock could be acquired again only after the expiration period set by the `exprire` prameter is due. default: true
    * @return Returns Future[None] in case resouce is locked.
    *         Returns Future[Some[Future[T] ] ]  in case lock was successful, where Future[T] is the computed result of the concurrent task
    */
  def acquire[T](resourceId: String, expire: Duration = Duration.Inf, releaseLockWhenDone:Boolean = true, taskId:Option[String]=None)(synchronizedTask: => T): Future[Option[T]]

}
