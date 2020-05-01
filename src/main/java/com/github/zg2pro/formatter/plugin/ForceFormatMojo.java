package com.github.zg2pro.formatter.zg2pro.formatter.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.google.common.io.Files;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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
import org.ec4j.core.Cache.Caches;
import org.ec4j.lint.api.Resource;
import org.ec4j.core.Resource.Charsets;
import org.ec4j.core.Resource.Resources;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.lint.api.FormattingHandler;
import org.ec4j.lint.api.Linter;
import org.ec4j.lint.api.ViolationHandler;
import org.ec4j.linters.TextLinter;
import org.ec4j.linters.XmlLinter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.LoggerFactory;

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

        executeEditorConfigOnGitRepo(rootDirectory);
    }

    private void executeEditorConfigOnGitRepo(String rootDirectory)
            throws MojoExecutionException {
        try {
            File projectBaseDir = new File(rootDirectory);
            Git git = Git.open(projectBaseDir);
            Repository repo = git.getRepository();
            IgnoreRules ir = new IgnoreRules(repo);

            final ViolationHandler handler = new FormattingHandler(false, ".bak",
                    new LoggerWrapper(LoggerFactory.getLogger(FormattingHandler.class)));
            final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService.builder() //
                    .cache(Caches.permanent()) //
                    .build();
            final ResourceProperties editorConfigProperties = resourcePropertiesService
                    .queryProperties(Resources.ofPath(projectBaseDir.toPath().resolve(".editorconfig"), StandardCharsets.UTF_8));
            handler.startFiles();

            File currentModuleFolder = project.getFile().getParentFile();
            List<String> subModules = project.getModules();

            for (File insideModule : currentModuleFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !subModules.contains(name);
                }
            })) {
                handleFile(insideModule, ir, handler, editorConfigProperties);
            }

        } catch (IOException ex) {
            throw new MojoExecutionException(
                    "could not open this folder with jgit",
                    ex
            );
        }
    }

    private boolean isBinaryFile(File f) throws IOException {
        String type = java.nio.file.Files.probeContentType(f.toPath());
        return type == null || !type.startsWith("text");
    }
    
    private void handleFile(File f, IgnoreRules ir,
            ViolationHandler handler,
            ResourceProperties editorConfigProperties) throws IOException {
        if (f.isDirectory()) {
            for (File insideFolder : f.listFiles()) {
                handleFile(insideFolder, ir, handler, editorConfigProperties);
            }
        } else {
            if (!ir.isIgnored(f) && !isBinaryFile(f)) {
                formatWithEditorconfig(f, handler, editorConfigProperties);
            }
        }
    }

    private void formatWithEditorconfig(File file,
            ViolationHandler handler, ResourceProperties editorConfigProperties) throws IOException {
        getLog().debug("Processing file '{}'" + file.getPath());
        if (!editorConfigProperties.getProperties().isEmpty()) {
            final Charset useEncoding = Charsets
                    .forName(editorConfigProperties.getValue(PropertyType.charset, "UTF-8", true));
            final Resource resource = new Resource(file.toPath(), file.toPath(), useEncoding);
            //the file can be in index but removed by the developer
            if (resource.getPath().toFile().exists()) {
                final List<Linter> filteredLinters = Arrays.asList(new XmlLinter(), new TextLinter());
                ViolationHandler.ReturnState state = ViolationHandler.ReturnState.RECHECK;
                while (state != ViolationHandler.ReturnState.FINISHED) {
                    for (Linter linter : filteredLinters) {
                        getLog().debug("Processing file using linter " + linter.getClass().getName());
                        handler.startFile(resource);
                        linter.process(resource, editorConfigProperties, handler);
                    }
                    state = handler.endFile();
                }
            }
        }
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
        } else {
            getLog().debug("linux recognized, checking end of lines in editorconfig");
            content = content.replace("end_of_line = crlf", "end_of_line = lf");
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
