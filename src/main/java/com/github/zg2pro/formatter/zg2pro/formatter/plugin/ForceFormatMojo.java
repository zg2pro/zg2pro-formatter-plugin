package com.github.zg2pro.formatter.zg2pro.formatter.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.google.common.io.Files;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog()
                    .info(
                            "Skipping plugin execution (actually repositionning git original hook)"
                    );
            gitHookPluginExecution(".git/hooks");
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
                checkFileAndOverwriteIfNeedBe(".editorconfig");
                getLog().debug("pre-commit.sh");
                checkFileAndOverwriteIfNeedBe(".hooks/pre-commit.sh");
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "could not write the .editorconfig file at root of project",
                        e
                );
            }
            getLog().info("executes git hook placement");
            gitHookPluginExecution(".hooks");
        } else {
            getLog().debug("removal of other instance of .editorconfig in submodules");
            File currentModuleEditorConfig = project.getFile().getParentFile().toPath().resolve(".editorconfig").toFile();
            if (currentModuleEditorConfig.exists()) {
                getLog().debug("found one, deleting");
                currentModuleEditorConfig.delete();
            }
        }
        getLog().info("executes prettier java");
        executeMojo(
                plugin(
                        groupId("com.hubspot.maven.plugins"),
                        artifactId("prettier-maven-plugin"),
                        version("0.6")
                ),
                goal("write"),
                configuration(),
                executionEnvironment(project, session, pluginManager)
        );

        getLog().info("executes editorconfig");
        executeMojo(
                plugin(
                        groupId("org.ec4j.maven"),
                        artifactId("editorconfig-maven-plugin"),
                        version("0.0.11")
                ),
                goal("format"),
                configuration(
                        element(
                                name("excludes"),
                                element("exclude", ".git/**"),
                                element("exclude", "**/dist/**"),
                                element("exclude", "**/node_modules/**"),
                                element("exclude", "**/package-lock.json")
                        ),
                        element(
                                name("includes"),
                                element("include", "**/*.java"),
                                element("include", "**/*.js"),
                                element("include", "**/*.json"),
                                element("include", "**/*.yml"),
                                element("include", "**/*.properties"),
                                element("include", "**/*.vue"),
                                element("include", "**/*.xml")
                        )
                ),
                executionEnvironment(project, session, pluginManager)
        );

    }

    private void gitHookPluginExecution(String position)
            throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("com.rudikershaw.gitbuildhook"),
                        artifactId("git-build-hook-maven-plugin"),
                        version("3.0.0")
                ),
                goal("configure"),
                configuration(
                        element(name("gitConfig"), element("core.hooksPath", position))
                ),
                executionEnvironment(project, session, pluginManager)
        );
    }

    private void checkFileAndOverwriteIfNeedBe(String filepath)
            throws IOException {
        InputStream ecStream = this.getClass().getClassLoader().getResourceAsStream(filepath);
        byte[] targetArray = new byte[ecStream.available()];
        ecStream.read(targetArray);
        String content = new String(targetArray, "UTF-8");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            getLog().debug("windows recognized, checking end of lines in editorconfig");
            content = content.replace("end_of_line = lf", "end_of_line = crlf");
        }
        File couldBeExistingFile = project
                .getFile()
                .getParentFile()
                .toPath()
                .resolve(filepath)
                .toFile();
        if (!couldBeExistingFile.exists()
                || couldBeExistingFile.exists()
                && !Arrays.equals(content.getBytes("UTF-8"), Files.toByteArray(couldBeExistingFile))) {
            getLog().info("overwrites " + filepath);
            FileUtils.writeStringToFile(couldBeExistingFile, content, "UTF-8");
        }
    }
}
