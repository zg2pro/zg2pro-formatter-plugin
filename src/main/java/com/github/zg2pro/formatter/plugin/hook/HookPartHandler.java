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
package com.github.zg2pro.formatter.plugin.hook;

import static com.github.zg2pro.formatter.plugin.util.DependenciesVersions.GIT_HOOK_MAVEN_PLUGIN_VERSION;
import com.github.zg2pro.formatter.plugin.util.FileOverwriter;
import java.io.IOException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
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

/**
 *
 * @author zg2pro
 */
public class HookPartHandler {

    private MavenProject project;

    private MavenSession session;

    private BuildPluginManager pluginManager;
    private FileOverwriter fileOverwriter;

    public HookPartHandler(MavenProject project, MavenSession session, BuildPluginManager pluginManager, FileOverwriter fileOverwriter) {
        this.project = project;
        this.session = session;
        this.pluginManager = pluginManager;
        this.fileOverwriter = fileOverwriter;
    }

    public void overwriteCommitHook() throws IOException {
        String hookfilename = ".hooks/pre-commit";
        fileOverwriter.checkFileAndOverwriteIfNeedBe(project.getFile(), hookfilename);
        if (!project.getFile()
                .getParentFile()
                .toPath()
                .resolve(hookfilename)
                .toFile().setExecutable(true)) {
            throw new IllegalStateException("please make your " + hookfilename + " executable");
        }
    }

    public void gitHookPluginExecution(String position)
            throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("com.rudikershaw.gitbuildhook"),
                        artifactId("git-build-hook-maven-plugin"),
                        version(GIT_HOOK_MAVEN_PLUGIN_VERSION)
                ),
                goal("configure"),
                configuration(
                        element(name("gitConfig"), element("core.hooksPath", position))
                ),
                executionEnvironment(project, session, pluginManager)
        );
    }

}
