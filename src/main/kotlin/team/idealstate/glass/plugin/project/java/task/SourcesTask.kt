package team.idealstate.glass.plugin.project.java.task

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
abstract class SourcesTask : Copy() {

    @Input
    val sourceSet = project.objects.property(SourceSet::class.java).apply {  finalizeValueOnRead()  }

    init {
        val sourceSet = sourceSet.get()
        super.into(project.layout.buildDirectory.dir("glass/sources/${sourceSet.name}"))
        super.from(sourceSet.allSource)
    }
}