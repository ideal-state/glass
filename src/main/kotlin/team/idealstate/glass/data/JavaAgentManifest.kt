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

package team.idealstate.glass.data

data class JavaAgentManifest(
    var premain: String = "",
    var agentmain: String = "",
    var canRedefineClasses: Boolean = false,
    var canRetransformClasses: Boolean = false,
    var canSetNativeMethodPrefix: Boolean = false,
) {

    fun toManifestAttributes(): Map<String, *> {
        val attributes = linkedMapOf<String, Any>()
        if (premain.isNotBlank()) {
            attributes["Premain-Class"] = premain
        }
        if (agentmain.isNotBlank()) {
            attributes["Agent-Class"] = agentmain
        }
        if (attributes.isEmpty()) {
            return attributes
        }
        attributes["Can-Redefine-Classes"] = canRedefineClasses
        attributes["Can-Retransform-Classes"] = canRetransformClasses
        attributes["Can-Set-Native-Method-Prefix"] = canSetNativeMethodPrefix
        return attributes
    }
}
