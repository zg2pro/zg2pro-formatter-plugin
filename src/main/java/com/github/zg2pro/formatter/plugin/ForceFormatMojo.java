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
        hookHandler = new HookPartHandler(project, session, pluginManager, fileOverwriter);
        prettierHandler = new PrettierPartHandler(project, session, pluginManager);
        editorconfigHandler = new EditorConfigPartHandler(project, fileOverwriter);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initServices();
        if (skip) {
            getLog()
                    .info(
                            "Skipping plugin execution (actually repositionning git original hook)"
                    );
            hookHandler.gitHookPluginExecution(".git/hooks/");
            return;
        }
        String rootDirectory = session.getExecutionRootDirectory();
        String rootDirectoryPom = rootDirectory + File.separatorChar + "pom.xml";
        getLog().debug("rootDirectoryPom:" + rootDirectoryPom);
        String currentModulePom = project.getFile().toString();
        getLog().debug("currentModulePom:" + currentModulePom);
        if (StringUtils.equals(rootDirectoryPom, currentModulePom)) {
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
            getLog().info("executes git hook placement");
            hookHandler.gitHookPluginExecution(".hooks/");
        } else {
            getLog().debug("removal of other instance of .editorconfig in submodules");
            editorconfigHandler.cleanEditorconfigsInSubmodules();
        }
        getLog().info("executes prettier java");
        prettierHandler.prettify();

        getLog().info("executes editorconfig");
        editorconfigHandler.executeEditorConfigOnGitRepo(rootDirectory);
    }

}
