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
package com.github.zg2pro.formatter.plugin.editorconfig;

import com.github.zg2pro.formatter.plugin.AbstractFormatterService;
import com.github.zg2pro.formatter.plugin.util.FileOverwriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tika.Tika;
import org.ec4j.core.Cache;
import org.ec4j.core.Resource;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.lint.api.FormattingHandler;
import org.ec4j.lint.api.Linter;
import org.ec4j.lint.api.ViolationHandler;
import org.ec4j.linters.TextLinter;
import org.ec4j.linters.XmlLinter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zg2pro
 */
public class EditorConfigPartHandler extends AbstractFormatterService {
    private final MavenProject project;
    private final FileOverwriter fileOverwriter;
    private final Linter textLinter = new TextLinter();
    private final Linter xmlLinter = new XmlLinter();
    private final Tika tika = new Tika();
    private boolean applyEditorconfigOnlyWhenModified;

    public EditorConfigPartHandler(
        MavenProject project,
        FileOverwriter fileOverwriter,
        boolean applyEditorconfigOnlyWhenModified,
        Log logger
    ) {
        super(logger);
        this.project = project;
        this.fileOverwriter = fileOverwriter;
        this.applyEditorconfigOnlyWhenModified =
            applyEditorconfigOnlyWhenModified;
    }

    public void overwriteEditorconfig() throws IOException {
        fileOverwriter.checkFileAndOverwriteIfNeedBe(
            project.getFile(),
            "editorconfig",
            getLog()
        );
    }

    public void cleanEditorconfigsInSubmodules() {
        File currentModuleEditorConfig = project
            .getFile()
            .getParentFile()
            .toPath()
            .resolve(".editorconfig")
            .toFile();
        if (currentModuleEditorConfig.exists()) {
            getLog().debug("found one, deleting");
            currentModuleEditorConfig.delete();
        }
    }

    public void executeEditorConfigOnGitRepo(
        File projectBaseDir,
        Git git,
        Repository repo
    )
        throws MojoExecutionException {
        try {
            IgnoreRules ir = new IgnoreRules(repo);

            final ViolationHandler handler = new FormattingHandler(
                false,
                ".bak",
                new LoggerWrapper(
                    LoggerFactory.getLogger(FormattingHandler.class)
                )
            );
            final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService
                .builder() //
                .cache(Cache.Caches.permanent()) //
                .build();
            final ResourceProperties editorConfigProperties = resourcePropertiesService.queryProperties(
                Resource.Resources.ofPath(
                    projectBaseDir.toPath().resolve(".editorconfig"),
                    StandardCharsets.UTF_8
                )
            );
            handler.startFiles();

            File currentModuleFolder = project.getFile().getParentFile();
            List<String> subModules = project.getModules();

            Status gitStatus = git.status().call();
            final Set<String> modified = new HashSet<>(gitStatus.getModified());
            modified.addAll(gitStatus.getChanged());
            for (File insideModule : currentModuleFolder.listFiles(
                (File dir, String name) -> !subModules.contains(name)
            )) {
                handleFile(
                    insideModule,
                    ir,
                    handler,
                    editorConfigProperties,
                    modified
                );
            }
        } catch (GitAPIException | NoWorkTreeException | IOException ex) {
            throw new MojoExecutionException(
                "could not open this folder with jgit",
                ex
            );
        }
    }

    private static final Map<String, Boolean> FILETYPES_ARE_XML = new HashMap<>();

    static {
        FILETYPES_ARE_XML.put("text", false);
        for (String xmls : new String[] {
            "xml",
            "xsl",
            "html",
            "xhtml"/*,
            "x-yaml",
            "yaml"*/
        }) {
            FILETYPES_ARE_XML.put("application/" + xmls, true);
            FILETYPES_ARE_XML.put("text/" + xmls, true);
        }
        FILETYPES_ARE_XML.put("application/sql", false);
        FILETYPES_ARE_XML.put("application/graphql", false);
        FILETYPES_ARE_XML.put("application/ld+json", false);
        FILETYPES_ARE_XML.put("application/javascript", false);
        FILETYPES_ARE_XML.put("application/json", false);
        FILETYPES_ARE_XML.put("application/x-sh", false);
        FILETYPES_ARE_XML.put("application/x-bash", false);
    }

    private boolean containsModifications(File f, Set<String> modifiedFiles) {
        return modifiedFiles
            .stream()
            .anyMatch(
                modifiedFile ->
                    (
                        f
                            .getAbsolutePath()
                            .replaceAll("\\\\", "/")
                            .contains(modifiedFile)
                    )
            );
    }

    private boolean isBinaryFile(File f, StringBuilder sbLogs)
        throws IOException {
        String type = tika.detect(f);
        sbLogs.append(" | filetype: " + type);
        boolean binary = true;
        if (type != null) {
            if (
                !FILETYPES_ARE_XML
                    .keySet()
                    .stream()
                    .noneMatch(accepted -> (type.startsWith(accepted)))
            ) {
                return false;
            }
        }
        return binary;
    }

    private boolean isXmlFile(File f) throws IOException {
        String type = tika.detect(f);

        boolean isXml = false;
        if (type != null) {
            for (Map.Entry<String, Boolean> accepted : FILETYPES_ARE_XML.entrySet()) {
                if (type.startsWith(accepted.getKey())) {
                    return accepted.getValue();
                }
            }
        }
        return isXml;
    }

    private void handleFile(
        File f,
        IgnoreRules ir,
        ViolationHandler handler,
        ResourceProperties editorConfigProperties,
        Set<String> modifiedFiles
    )
        throws IOException {
        if (f.isDirectory()) {
            if (!ir.isIgnored(f)) {
                for (File insideFolder : f.listFiles()) {
                    handleFile(
                        insideFolder,
                        ir,
                        handler,
                        editorConfigProperties,
                        modifiedFiles
                    );
                }
            }
        } else {
            StringBuilder sbLogs = new StringBuilder(
                "Found a file: " + f.getPath()
            );
            if (
                !ir.isIgnored(f) &&
                !isBinaryFile(f, sbLogs) &&
                (
                    !applyEditorconfigOnlyWhenModified ||
                    containsModifications(f, modifiedFiles)
                )
            ) {
                formatWithEditorconfig(
                    f,
                    handler,
                    editorConfigProperties,
                    sbLogs
                );
            }
            getLog().debug(sbLogs.toString());
        }
    }

    private void formatWithEditorconfig(
        File file,
        ViolationHandler handler,
        ResourceProperties editorConfigProperties,
        StringBuilder sbLogs
    )
        throws IOException {
        if (!editorConfigProperties.getProperties().isEmpty()) {
            final Charset useEncoding = Resource.Charsets.forName(
                editorConfigProperties.getValue(
                    PropertyType.charset,
                    "UTF-8",
                    true
                )
            );
            final org.ec4j.lint.api.Resource resource = new org.ec4j.lint.api.Resource(
                file.toPath(),
                file.toPath(),
                useEncoding
            );
            //the file can be in index but removed by the developer
            if (resource.getPath().toFile().exists()) {
                ViolationHandler.ReturnState state =
                    ViolationHandler.ReturnState.RECHECK;
                int maxFourLoopsOnSameFile = 4;
                while (state != ViolationHandler.ReturnState.FINISHED) {
                    handler.startFile(resource);
                    if (isXmlFile(resource.getPath().toFile())) {
                        sbLogs.append(" | linting file as xml");
                        xmlLinter.process(
                            resource,
                            editorConfigProperties,
                            handler
                        );
                    }
                    sbLogs.append(" | linting file as text");
                    textLinter.process(
                        resource,
                        editorConfigProperties,
                        handler
                    );
                    state = handler.endFile();
                    maxFourLoopsOnSameFile--;
                    if (maxFourLoopsOnSameFile < 1) {
                        state = ViolationHandler.ReturnState.FINISHED;
                    }
                }
            }
        }
    }
}
