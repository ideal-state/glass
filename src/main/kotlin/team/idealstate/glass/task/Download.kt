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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@CacheableTask
abstract class Download : DefaultTask() {
    @Input
    val url = project.objects.property(URI::class.java).apply { finalizeValueOnRead() }

    @Input
    val connectTimeout = project.objects.property(Long::class.java).apply { finalizeValueOnRead() }

    @OutputFile
    val destinationFile = project.objects.property(File::class.java).apply { finalizeValueOnRead() }

    @TaskAction
    protected open fun download() {
        val uri = url.get()
        val file = destinationFile.get()
        val client =
            HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMillis(connectTimeout.orNull ?: 5000L))
                .build()
        val request =
            HttpRequest
                .newBuilder()
                .uri(uri)
                .GET()
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(file.toPath()))
        val statusCode = response.statusCode()
        if (statusCode != HttpURLConnection.HTTP_OK) {
            throw IllegalStateException("Failed to download \"$uri\". [status code: $statusCode]")
        }
    }
}
