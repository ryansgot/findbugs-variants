package com.fsryan.gradle.findbugsvariants

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.FindBugs

class FindbugsVariantsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.afterEvaluate {
            def findbugsTask = project.tasks.create(name: "findbugs", description: "findbugs for all build variants", group: "verification")
            project.tasks.getByName('check').dependsOn(findbugsTask)

            findVariants(project).each { variant ->
                def task = project.tasks.create("findBugs${variant.name.capitalize()}", FindBugs)
                task.group = 'verification'
                task.description = "findbugs for ${variant.description}"

                task.ignoreFailures = false
                task.effort = 'max'
                task.reportLevel = 'low'

                // filter files
                final File excludeFilterFile = project.file("${project.projectDir}${File.separator}findbugs${File.separator}${variant.name}/exclude-filter.xml")
                final File excludeBugsFilterFile = project.file("${project.projectDir}${File.separator}findbugs${File.separator}${variant.name}/exclude-bugs-filter.xml")
                final File includeFilterFile = project.file("${project.projectDir}${File.separator}findbugs${File.separator}${variant.name}/include-filter.xml")
                if (excludeFilterFile.exists()) {
                    task.excludeFilter = excludeFilterFile
                } else {
                    println "$excludeFilterFile does not exist--not using findbugs exclude filter for ${variant.name}"
                }
                if (excludeBugsFilterFile.exists()) {
                    task.excludeBugsFilter = excludeBugsFilterFile
                } else {
                    println "$excludeBugsFilterFile does not exist--not using findbugs exclude bugs filter for ${variant.name}"
                }
                if (includeFilterFile.exists()) {
                    task.includeFilter = includeFilterFile
                } else {
                    println "$includeFilterFile does not exist--not using findbugs include filter for ${variant.name}"
                }

                task.reports {
                    xml {
                        enabled = false
                        withMessages = true
                    }
                    html {
                        enabled = true
                    }
                }

                final File stylesheetFile = project.file("${project.projectDir}${File.separator}findbugs${File.separator}stylesheet.xsl")
                if (stylesheetFile.exists()) {
                    task.html.stylesheet = project.resources.text.fromFile(stylesheetFile)
                }

                def variantCompile = variant.javaCompile
                task.classes = project.fileTree(variantCompile.destinationDir)
                task.source = variantCompile.source
                task.classpath = variantCompile.classpath.plus(project.files(project.android.bootClasspath))
                task.dependsOn(variantCompile)

                findbugsTask.dependsOn task
            }
        }
    }

    static def findVariants(Project project) {
        if (project.android == null) {
            return []
        }
        return project.hasProperty('applicationVariants') ? project.android.applicationVariants : project.android.libraryVariants
    }
}
