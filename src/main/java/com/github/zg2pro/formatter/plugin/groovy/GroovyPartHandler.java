/*
 * The MIT License
 *
 * Copyright 2020 Gregory Anne.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.zg2pro.formatter.plugin.groovy;

import static com.github.zg2pro.formatter.plugin.util.DependenciesVersions.SPOTLESS_SCALA_MAVEN_PLUGIN_VERSION;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 *
 * @author zg2pro
 */
public class GroovyPartHandler {
    private final MavenProject project;

    private final MavenSession session;

    private final BuildPluginManager pluginManager;

    public GroovyPartHandler(
        MavenProject project,
        MavenSession session,
        BuildPluginManager pluginManager
    ) {
        this.project = project;
        this.session = session;
        this.pluginManager = pluginManager;
    }

    private static final MojoExecutor.Element groovyConfigElement = element(
        name("groovy"),
        element(name("greclipse"), ""),
        element(
            name("includes"),
            element(name("include"), "**/*.groovy"),
            element(name("include"), "**/Jenkinsfile")
        )
    );

    public void prettify() throws MojoExecutionException {
        executeMojo(
            plugin(
                groupId("com.diffplug.spotless"),
                artifactId("spotless-maven-plugin"),
                version(SPOTLESS_SCALA_MAVEN_PLUGIN_VERSION)
            ),
            goal("apply"),
            configuration(groovyConfigElement),
            executionEnvironment(project, session, pluginManager)
        );
    }
}
