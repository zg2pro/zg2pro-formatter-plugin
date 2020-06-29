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
package com.github.zg2pro.formatter.plugin;

import com.github.zg2pro.formatter.plugin.editorconfig.EditorConfigPartHandler;
import com.github.zg2pro.formatter.plugin.hook.HookPartHandler;
import com.github.zg2pro.formatter.plugin.prettier.PrettierPartHandler;
import com.github.zg2pro.formatter.plugin.util.FileOverwriter;
import java.io.File;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

/**
 * @author zg2pro
 */
@Mojo(defaultPhase = LifecyclePhase.VALIDATE, name = "apply", threadSafe = true)
public class ForceFormatMojo extends AbstractMojo {
    @Parameter(defaultValue = "false", property = "zg2pro.format.skip")
    private boolean skip;

    @Component
    public MavenProject project;

    @Component
    public MavenSession session;

    @Component
    public BuildPluginManager pluginManager;

    private HookPartHandler hookHandler;
    private PrettierPartHandler prettierHandler;
    private EditorConfigPartHandler editorconfigHandler;
    private FileOverwriter fileOverwriter;

    private void initServices() {
        fileOverwriter = new FileOverwriter();
        hookHandler =
            new HookPartHandler(
                project,
                session,
                pluginManager,
                fileOverwriter,
                skip
            );
        prettierHandler =
            new PrettierPartHandler(project, session, pluginManager);
        editorconfigHandler =
            new EditorConfigPartHandler(project, fileOverwriter);
    }

    private boolean handleSkipOption() throws MojoExecutionException {
        if (skip) {
            getLog()
                .info(
                    "Skipping plugin execution (actually repositionning git original hook)"
                );
        }
        return skip;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initServices();
        String rootDirectory = session.getExecutionRootDirectory();
        String rootDirectoryPom =
            rootDirectory + File.separatorChar + "pom.xml";
        File projectBaseDir = new File(rootDirectory);
        getLog().debug("rootDirectoryPom:" + rootDirectoryPom);
        String currentModulePom = project.getFile().toString();
        getLog().debug("currentModulePom:" + currentModulePom);
        Repository repo = null;
        boolean runningOnGitRepo = projectBaseDir
            .toPath()
            .resolve(".git")
            .toFile()
            .exists();
        try {
            if (runningOnGitRepo) {
                Git git = Git.open(projectBaseDir);
                repo = git.getRepository();
                getLog().info("executes git hook placement");
                hookHandler.gitHookPluginExecution(repo);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(
                "could not open this folder with jgit",
                ex
            );
        }
        if (handleSkipOption()) {
            return;
        }
        if (
            runningOnGitRepo &&
            StringUtils.equals(rootDirectoryPom, currentModulePom)
        ) {
            getLog().info("handling multimodule root directory setup");
            try {
                getLog().debug("editorconfig");
                editorconfigHandler.overwriteEditorconfig();
                getLog().debug("pre-commit");
                hookHandler.overwriteCommitHook();
            } catch (IOException e) {
                throw new MojoExecutionException(
                    "could not write the .editorconfig file at root of project",
                    e
                );
            }
        } else {
            getLog()
                .debug(
                    "removal of other instance of .editorconfig in submodules"
                );
            editorconfigHandler.cleanEditorconfigsInSubmodules();
        }
        getLog().info("executes prettier java");
        prettierHandler.prettify();

        if (repo != null) {
            getLog().info("executes editorconfig");
            editorconfigHandler.executeEditorConfigOnGitRepo(
                projectBaseDir,
                repo
            );
        }
    }
}
