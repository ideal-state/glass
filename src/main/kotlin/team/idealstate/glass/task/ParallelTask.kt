/*
 *    Copyright 2024 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.glass.task

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import team.idealstate.glass.context.parallel.Job
import team.idealstate.glass.context.parallel.JobContainer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

abstract class ParallelTask<K : Any, T : Any, R, J : Job<K, T, R>, C : JobContainer<K, T, R, J>> : DefaultTask() {
    companion object {
        const val DEFAULT_JOB_TIMEOUT = 30L

        @JvmStatic
        val JOB_TIMEOUT_UNIT = TimeUnit.SECONDS
    }

    @get:Internal
    protected val locks = ConcurrentHashMap<K, Lock>()

    @get:Internal
    protected abstract val jobs: C

    @get:Internal
    protected val jobResults = ConcurrentSkipListSet<R>()

    init {
        group = "parallel"
    }

    @Input
    @Optional
    val jobTimeout: Property<Long> =
        project.objects.property(Long::class.java).apply {
            set(DEFAULT_JOB_TIMEOUT)
        }

    fun jobs(action: Action<in C>) {
        action.execute(jobs)
    }

    @TaskAction
    protected open fun executeJobs() {
        val jobs = HashSet(this.jobs.all)
        if (jobs.isEmpty()) return
        if (jobs.size == 1) {
            jobs.first().execute()
            return
        }
        val parallel = jobs.parallelStream()
        val timeout = this.jobTimeout.orNull
        parallel.forEach { job ->
            val lock =
                locks.computeIfAbsent(job.key) {
                    ReentrantLock()
                }
            if (timeout != null) {
                if (!lock.tryLock(timeout, JOB_TIMEOUT_UNIT)) {
                    throw TimeoutException("Timeout while waiting for lock on '${job.key}'.")
                }
            } else {
                lock.lock()
            }
            try {
                val jobResult = job.execute()
                jobResult?.apply(jobResults::add)
            } finally {
                lock.unlock()
            }
        }
    }
}
