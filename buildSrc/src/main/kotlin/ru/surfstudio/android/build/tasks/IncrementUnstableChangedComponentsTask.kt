package ru.surfstudio.android.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import ru.surfstudio.android.build.Components
import ru.surfstudio.android.build.GradleProperties.COMPONENTS_CHANGED_REVISION_TO_COMPARE
import ru.surfstudio.android.build.model.Component
import ru.surfstudio.android.build.model.json.ComponentJson
import ru.surfstudio.android.build.tasks.changed_components.ComponentsConfigurationChecker
import ru.surfstudio.android.build.tasks.changed_components.ComponentsFilesChecker
import ru.surfstudio.android.build.tasks.changed_components.GitCommandRunner
import ru.surfstudio.android.build.utils.JsonHelper
import ru.surfstudio.android.build.tasks.changed_components.models.ComponentCheckResult
import ru.surfstudio.android.build.utils.COMPONENTS_JSON_FILE_PATH
import java.io.File

/**
 * Task for incrementing unstable version parameter in components.json for the component that is unstable was changed
 * between current revision and [revisionToCompare]
 */
open class IncrementUnstableChangedComponentsTask : DefaultTask() {

    private lateinit var revisionToCompare: String

    @TaskAction
    fun increment() {
        extractInputArguments()
        val currentRevision = GitCommandRunner().getCurrentRevisionShort()

        println("DEV-INFO revisionToCompare ${revisionToCompare}")
        println("DEV-INFO currentRevision ${currentRevision}")

        val resultByFiles = ComponentsFilesChecker(currentRevision, revisionToCompare)
                .getChangeInformationForComponents()

        resultByFiles.forEach {
            println("DEV-INFO resultByFiles ${it.componentName}=${it.isComponentChanged}")
        }

        val resultsByConfiguration = ComponentsConfigurationChecker(currentRevision, revisionToCompare)
                .getChangeInformationForComponents()

        resultsByConfiguration.forEach {
            println("DEV-INFO resultsByConfiguration ${it.componentName}=${it.isComponentChanged}")
        }

        println("DEV-INFO Components Before ${Components.value}")
        incrementUnstableChanged(resultByFiles, resultsByConfiguration)
        println("DEV-INFO Components After ${Components.value}")
    }

    private fun extractInputArguments() {
        if (!project.hasProperty(COMPONENTS_CHANGED_REVISION_TO_COMPARE)) {
            throw GradleException("please specify $COMPONENTS_CHANGED_REVISION_TO_COMPARE param")
        }
        revisionToCompare = project.findProperty(COMPONENTS_CHANGED_REVISION_TO_COMPARE) as String
    }

    private fun incrementUnstableChanged(resultByFiles: List<ComponentCheckResult>, resultByConfigurations: List<ComponentCheckResult>) {
        val currentComponents = Components.value

        val newComponents = currentComponents
                .map { component ->
                    val resultByFile = resultByFiles.find { it.componentName == component.name }
                    val resultByConfig = resultByConfigurations.find { it.componentName == component.name }

                    if (resultByConfig == null || resultByFile == null) {
                        throw GradleException("one of the results doesn`t contain information about component ${component.name}")
                    }

                    if (isComponentUnstableAndChanged(component, resultByFile, resultByConfig)) {
                        component.copy(unstableVersion = component.unstableVersion + 1)
                    } else {
                        component.copy()
                    }
                }
        writeNewComponentsToFile(newComponents)
    }

    private fun writeNewComponentsToFile(newComponents: List<Component>) {
        JsonHelper.write(
                newComponents.map { ComponentJson(it) },
                File("$currentDirectory/$COMPONENTS_JSON_FILE_PATH")
        )
        Components.value = newComponents //fix reuse process with old parsed components for next tasks
    }

    private fun isComponentUnstableAndChanged(
            component: Component,
            resultByFile: ComponentCheckResult,
            resultByConfig: ComponentCheckResult
    ): Boolean {
        return !component.stable && (resultByFile.isComponentChanged || resultByConfig.isComponentChanged)
    }
}