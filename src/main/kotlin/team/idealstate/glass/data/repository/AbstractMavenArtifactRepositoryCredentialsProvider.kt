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

package team.idealstate.glass.data.repository

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.PasswordCredentials
import team.idealstate.glass.data.CredentialsProvider

abstract class AbstractMavenArtifactRepositoryCredentialsProvider(
    private val repository: MavenArtifactRepository,
) : CredentialsProvider<MavenArtifactRepository> {
    protected companion object {
        const val REPOSITORY_PROPERTY_PREFIX = "glass.publish"
        const val REPOSITORY_LOGIN_KEY = "key"
        const val REPOSITORY_LOGIN_SECRET = "secret"
        const val REPOSITORY_ID_DELIMITER = '.'

        private val CHARS =
            arrayOf(
                '-',
                '_',
            )

        fun normalize(name: String): String {
            var normalized = name
            for (char in CHARS) {
                normalized = normalized.replace(char, REPOSITORY_ID_DELIMITER)
            }
            return normalized.lowercase()
        }
    }

    protected val id = normalize(repository.name)
    protected val usernameId = "$REPOSITORY_PROPERTY_PREFIX.$id.$REPOSITORY_LOGIN_KEY"
    protected val passwordId = "$REPOSITORY_PROPERTY_PREFIX.$id.$REPOSITORY_LOGIN_SECRET"

    protected abstract fun login(credentials: PasswordCredentials)

    override fun login() {
        login(repository.credentials)
    }
}
