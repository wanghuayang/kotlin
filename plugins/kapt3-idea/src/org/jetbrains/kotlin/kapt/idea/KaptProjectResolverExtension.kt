/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt.idea

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractModuleDataService
import com.intellij.openapi.util.Key
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.Modifier

var DataNode<ModuleData>.kaptModel by UserDataProperty(Key.create<KaptGradleModel>("KAPT_MODEL"))

interface KaptSourceSetModel : Serializable {
    val sourceSetName: String
    val generatedSourcesDir: String
    val generatedClassesDir: String
    val generatedKotlinSourcesDir: String
}

class KaptSourceSetModelImpl(
        override val sourceSetName: String,
        override val generatedSourcesDir: String,
        override val generatedClassesDir: String,
        override val generatedKotlinSourcesDir: String
) : KaptSourceSetModel

interface KaptGradleModel : Serializable {
    val isEnabled: Boolean
    val sourceSets: List<KaptSourceSetModel>
}

class KaptGradleModelImpl(
        override val isEnabled: Boolean,
        override val sourceSets: List<KaptSourceSetModel>
) : KaptGradleModel

@Suppress("unused")
class KaptProjectResolverExtension : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(KaptGradleModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(KaptModelBuilderService::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val kaptModel = resolverCtx.getExtraProject(gradleModule, KaptGradleModel::class.java) ?: return

        ideModule.kaptModel = kaptModel

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}

class KaptModelBuilderService : AbstractKotlinGradleModelBuilder() {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
                .withDescription("Unable to build kotlin-kapt plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KaptGradleModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val kaptPlugin = project.plugins.findPlugin("kotlin-kapt")
        val kaptIsEnabled = kaptPlugin != null

        val sourceSets = mutableListOf<KaptSourceSetModel>()

        if (kaptIsEnabled) {
            project.getAllTasks(false)[project]?.forEach { compileTask ->
                if (compileTask.javaClass.name !in kotlinCompileTaskClasses) return@forEach

                val sourceSetName = compileTask.getSourceSetName()

                val kaptGeneratedSourcesDir = getKaptDirectory("getKaptGeneratedSourcesDir", project, sourceSetName)
                val kaptGeneratedClassesDir = getKaptDirectory("getKaptGeneratedClassesDir", project, sourceSetName)
                val kaptGeneratedKotlinSourcesDir = getKaptDirectory("getKaptGeneratedKotlinSourcesDir", project, sourceSetName)
                sourceSets += KaptSourceSetModelImpl(sourceSetName, kaptGeneratedSourcesDir, kaptGeneratedClassesDir, kaptGeneratedKotlinSourcesDir)
            }
        }

        return KaptGradleModelImpl(kaptIsEnabled, sourceSets)
    }

    private fun getKaptDirectory(funName: String, project: Project, sourceSetName: String): String {
        val kotlinKaptPlugin = project.plugins.findPlugin("kotlin-kapt") ?: return ""

        val targetMethod = kotlinKaptPlugin::class.java.declaredMethods.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.name == funName && it.parameterCount == 2
        } ?: return ""

        return (targetMethod(null, project, sourceSetName) as? File)?.absolutePath ?: ""
    }
}

class KaptModuleDataService : AbstractModuleDataService<ModuleData>() {
    override fun getTargetDataKey() = ProjectKeys.MODULE

    override fun importData(
            toImport: Collection<DataNode<ModuleData>>,
            projectData: ProjectData?,
            project: com.intellij.openapi.project.Project,
            modelsProvider: IdeModifiableModelsProvider
    ) {
        for (moduleData in toImport) {
            val module = modelsProvider.findIdeModule(moduleData.data) ?: continue
            val modifiableModel = modelsProvider.getModifiableRootModel(module)
            val kaptModel = moduleData.kaptModel?.takeIf { it.isEnabled } ?: continue
        }

        super.importData(toImport, projectData, project, modelsProvider)
    }
}