/*
 * Copyright 2014-2017 Time Warner Cable, Inc.
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
package com.twcable.gradle

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.CachingTaskDependencyResolveContext

import javax.annotation.Nonnull

@CompileStatic
class GradleUtils {

    static <T> T extension(Project project, Class<T> confClass, Object... args) {
        def conf = project.extensions.findByType(confClass)
        if (conf != null) return conf

        def confName = confClass.getDeclaredField("NAME").get(null) as String
        project.logger.debug "Creating extension \"${confName}\" with ${confClass.name} with ${args}"
        return project.extensions.create(confName, confClass, args)
    }

    /**
     * Execute a task with its dependencies
     */
    static void execute(@Nonnull Task task) {
        ensureProjectEvaluated(task)
        def context = new CachingTaskDependencyResolveContext()
        executeWithCtx(task, context)
    }


    private static void ensureProjectEvaluated(Task task) {
        final project = task.project as ProjectInternal
        if (!project.getState().executing /* implies that it's neither executed nor executing */) project.evaluate()
    }


    private static void executeWithCtx(@Nonnull Task task, @Nonnull CachingTaskDependencyResolveContext context) {
        def dependencies = context.getDependencies(task) as Set<Task>

        dependencies.each { Task depTask ->
            executeWithCtx(depTask, context)
        }

        if (task instanceof TaskInternal)
            ((TaskInternal)task).execute()
        else
            throw new GradleException("Don't know what to do with ${task} - ${task.getClass().name}")
    }


    @TypeChecked(TypeCheckingMode.SKIP)
    @SuppressWarnings("GroovyUnusedDeclaration")
    static void taskDependencyGraph(Project project, Collection<Task> theTasks = null) {
        // TODO: Tie into the "Reporting" infrastructure of Gradle
        StringBuilder sb = new StringBuilder("digraph Compile {\n")
        sb.append("  node [style=filled, color=lightgray]\n")

        if (theTasks == null) {
            theTasks = project.tasks
        }

        for (Task task : theTasks) {
            sb.append("  \"${task.path}\" [style=rounded]\n")
            def context = new CachingTaskDependencyResolveContext()
            def dependencies = context.getDependencies(task)
            dependencies.each {
                sb.append("  \"${task.path}\"->\"${it.path}\"\n")
            }

            task.getShouldRunAfter().getDependencies(task).each {
                sb.append("  \"${task.path}\"->\"${it.path}\" [style=\"dotted\"]\n")
            }

            task.getMustRunAfter().getDependencies(task).each {
                sb.append("  \"${task.path}\"->\"${it.path}\" [style=\"dashed\"]\n")
            }
        }
        sb.append("}")

        def file = new File("tasks.dot")
        println "Writing to ${file.absolutePath}"
        file.write(sb.toString())
    }

}
