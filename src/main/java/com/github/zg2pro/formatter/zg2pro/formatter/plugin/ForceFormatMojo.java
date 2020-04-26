package com.github.zg2pro.formatter.zg2pro.formatter.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.google.common.io.Files;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
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

        try {
            //configuration to merge with in Xpp3dom:
//        <configuration>
//  <addLintersFromClassPath default-value="true" implementation="boolean">${editorconfig.addLintersFromClassPath}</addLintersFromClassPath>
//  <backup default-value="false" implementation="boolean">${editorconfig.backup}</backup>
//  <backupSuffix default-value=".bak" implementation="java.lang.String">${editorconfig.backupSuffix}</backupSuffix>
//  <basedir default-value="${project.basedir}" implementation="java.io.File"/>
//  <encoding default-value="${project.build.sourceEncoding}" implementation="java.lang.String">${editorconfig.encoding}</encoding>
//  <excludeNonSourceFiles default-value="true" implementation="boolean">${editorconfig.excludeNonSourceFiles}</excludeNonSourceFiles>
//  <excludeSubmodules default-value="true" implementation="boolean">${editorconfig.excludeSubmodules}</excludeSubmodules>
//  <excludes implementation="java.lang.String[]">${editorconfig.excludes}</excludes>
//  <failOnNoMatchingProperties default-value="true" implementation="boolean">${editorconfig.failOnNoMatchingProperties}</failOnNoMatchingProperties>
//  <includes default-value="**" implementation="java.lang.String[]">${editorconfig.includes}</includes>
//  <skip default-value="false" implementation="boolean">${editorconfig.skip}</skip>
//</configuration>
//        executeMojo(
//                plugin(
//                        groupId("org.ec4j.maven"),
//                        artifactId("editorconfig-maven-plugin"),
//                        version("0.0.11")
//                ),
//                goal("format"),
//                configuration(
//                     //   element(name("excludes"), ".git/**,**/target/**,**/dist/**,**/node_modules/**,**/node/**,**/package-lock.json")
//                    //    element(name("includes"), "**/*.java,**/*.js,**/*.json,**/*.ts,**/*.yml,**/*.properties,**.*.xml,**.*.vue")
//                        element(name("includes"), "**")
//                ),
//                executionEnvironment(project, session, pluginManager)
//        );
            File projectBaseDir = new File(rootDirectory);
            Path currentModulePath = project.getFile().getParentFile().toPath();
            String relativePathToModule = currentModulePath.toFile().getAbsolutePath()
                    .replace(projectBaseDir.getAbsolutePath(), "");
            Git git = Git.open(projectBaseDir);
            Repository repo = git.getRepository();
            Ref head = repo.findRef("HEAD");
            RevWalk walk = new RevWalk(repo);
            RevCommit commit = walk.parseCommit(head.getObjectId());
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            final ViolationHandler handler = new FormattingHandler(false, ".bak",
                    new LoggerWrapper(LoggerFactory.getLogger(FormattingHandler.class)));
            final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService.builder() //
                    .cache(Caches.permanent()) //
                    .build();
            handler.startFiles();
            while (treeWalk.next()) {
                String file = treeWalk.getPathString();
//at root module this should be empty string
                if (file.startsWith(relativePathToModule)) {
                    boolean inSubmodule = false;
                    getLog().debug(file); //relative paths to basedir
                    if ("pom".equals(project.getPackaging())) {
                        List<String> subModulesNames = project.getModules();
                        for (String subM : subModulesNames) {
                            if (file.startsWith(subM)) {
                                inSubmodule = true;
                                getLog().debug("in submodule:" + file);
                            }
                        }
                    }
                    if (!inSubmodule) {
                        formatWithEditorconfig(file, projectBaseDir, resourcePropertiesService, handler);
                    }
                }
            }

        } catch (IOException ex) {
            throw new MojoExecutionException(
                    "could not open this folder with jgit",
                    ex
            );
        }
    }

    private void formatWithEditorconfig(String file, File projectBaseDir, final ResourcePropertiesService resourcePropertiesService, final ViolationHandler handler) throws IOException {
        final Path filePath = Paths.get(file); // relative to basedir
        final Path absFile = projectBaseDir.toPath().resolve(filePath);
        getLog().debug("Processing file '{}'" + filePath);
        final ResourceProperties editorConfigProperties = resourcePropertiesService
                .queryProperties(Resources.ofPath(absFile, StandardCharsets.UTF_8));
        if (!editorConfigProperties.getProperties().isEmpty()) {
            final Charset useEncoding = Charsets
                    .forName(editorConfigProperties.getValue(PropertyType.charset, "UTF-8", true));
            final Resource resource = new Resource(absFile, filePath, useEncoding);
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
