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

import static com.github.zg2pro.formatter.plugin.util.DependenciesVersions.NODE_VERSION;

import com.github.zg2pro.formatter.plugin.AbstractFormatterService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 *
 * @author zg2pro
 */
public class GroovyPartHandler extends AbstractFormatterService {
    private PluginDescriptor pluginDescriptor;
    private MavenProject project;
    private RepositorySystemSession repositorySystemSession;
    private RepositorySystem repositorySystem;

    public GroovyPartHandler(
        PluginDescriptor pluginDescriptor,
        MavenProject project,
        RepositorySystemSession repositorySystemSession,
        RepositorySystem repositorySystem
    ) {
        this.pluginDescriptor = pluginDescriptor;
        this.project = project;
        this.repositorySystemSession = repositorySystemSession;
        this.repositorySystem = repositorySystem;
    }

    public void prettify() throws MojoExecutionException {
        Path nodeExecutable = resolveNodeExecutable();
        String npmExecutable = OperatingSystemFamily.WINDOWS.equals(
                determineOperatingSystemFamily()
            )
            ? "npm.cmd"
            : "npm";
        Path npmExec = nodeExecutable.getParent().resolve(npmExecutable);
        List<String> installGroovyFormatterCmd = new ArrayList<>();
        installGroovyFormatterCmd.add(toString(npmExec));
        installGroovyFormatterCmd.add("install");
        installGroovyFormatterCmd.add("-g");
        installGroovyFormatterCmd.add("npm-groovy-lint");
        try {
            executeCommand(installGroovyFormatterCmd);
        } catch (IOException ex) {
            throw new MojoExecutionException("could not execute command", ex);
        }
        installGroovyFormatterCmd = new ArrayList<>();
        installGroovyFormatterCmd.add("npm-groovy-lint");
        try {
            executeCommand(installGroovyFormatterCmd);
        } catch (IOException ex) {
            throw new MojoExecutionException("could not execute command", ex);
        }
    }

    private void executeCommand(List<String> command)
        throws IOException, MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Running " + command);
        }

        Process process = new ProcessBuilder(command.toArray(new String[0]))
            .directory(project.getBasedir())
            .start();
        try (
            InputStreamReader stdoutReader = new InputStreamReader(
                process.getInputStream(),
                StandardCharsets.UTF_8
            );
            BufferedReader stdout = new BufferedReader(stdoutReader);
            InputStreamReader stderrReader = new InputStreamReader(
                process.getErrorStream(),
                StandardCharsets.UTF_8
            );
            BufferedReader stderr = new BufferedReader(stderrReader)
        ) {
            String line;
            while ((line = stdout.readLine()) != null) {
                handlePrettierLogLine(line);
            }

            boolean noMatchingFiles = false;
            while ((line = stderr.readLine()) != null) {
                if (line.contains("No matching files.")) {
                    noMatchingFiles = true;
                } else if (line.contains("error")) {
                    getLog().error(line);
                } else {
                    getLog().warn(line);
                }
            }

            int status = process.waitFor();
            if (status != 0) {
                if (status == 2 && noMatchingFiles) {
                    getLog().info("No files found matching glob ");
                } else {
                    handlePrettierNonZeroExit(status);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error trying to run command", e);
        }
    }

    private void handlePrettierLogLine(String line) {
        // colorized lines have no changes
        if (line.endsWith("ms") && !COLORIZED_LINE.test(line)) {
            getLog().info("Reformatted file: " + line);
        }
    }

    private void handlePrettierNonZeroExit(int status)
        throws MojoExecutionException {
        throw new MojoExecutionException(
            "Error trying to format code with prettier-java: " + status
        );
    }

    private static final Predicate<String> COLORIZED_LINE = Pattern
        .compile("(\\x9B|\\x1B\\[)[0-?]*[ -/]*[@-~]")
        .asPredicate();

    // Convert Windows Path to Unix style
    private String toString(Path path) {
        return path.toString().replace("\\", "/");
    }

    protected Path resolveNodeExecutable() throws MojoExecutionException {
        Artifact nodeArtifact = new DefaultArtifact(
            pluginDescriptor.getGroupId(),
            pluginDescriptor.getArtifactId(),
            determineNodeClassifier(),
            "exe",
            pluginDescriptor.getVersion()
        );

        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolving node artifact " + nodeArtifact);
        }

        File nodeExecutable = resolve(nodeArtifact).getFile();
        if (!nodeExecutable.setExecutable(true, false)) {
            throw new MojoExecutionException(
                "Unable to make file executable " + nodeExecutable
            );
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolved node artifact to " + nodeExecutable);
        }

        return nodeExecutable.toPath();
    }

    private String determineNodeClassifier() throws MojoExecutionException {
        OperatingSystemFamily osFamily = determineOperatingSystemFamily();
        return "node-" + NODE_VERSION + "-" + osFamily.getShortName();
    }

    private OperatingSystemFamily determineOperatingSystemFamily()
        throws MojoExecutionException {
        String osFullName = System.getProperty("os.name");
        if (osFullName == null) {
            throw new MojoExecutionException("No os.name system property set");
        } else {
            osFullName = osFullName.toLowerCase();
        }

        if (osFullName.startsWith("linux")) {
            return OperatingSystemFamily.LINUX;
        } else if (osFullName.startsWith("mac os x")) {
            return OperatingSystemFamily.MAC_OS_X;
        } else if (osFullName.startsWith("windows")) {
            return OperatingSystemFamily.WINDOWS;
        } else {
            throw new MojoExecutionException("Unknown os.name " + osFullName);
        }
    }

    private enum OperatingSystemFamily {
        LINUX("linux"),
        MAC_OS_X("mac_os_x"),
        WINDOWS("windows");

        private String shortName;

        OperatingSystemFamily(String shortName) {
            this.shortName = shortName;
        }

        public String getShortName() {
            return shortName;
        }

        public FileAttribute<?>[] getGlobalPermissions() {
            if (this == WINDOWS) {
                return new FileAttribute<?>[0];
            } else {
                return new FileAttribute<?>[] {
                    PosixFilePermissions.asFileAttribute(GLOBAL_PERMISSIONS)
                };
            }
        }
    }

    private static final Set<PosixFilePermission> GLOBAL_PERMISSIONS = PosixFilePermissions.fromString(
        "rwxrwxrwx"
    );

    private Artifact resolve(Artifact artifact) throws MojoExecutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest()
            .setArtifact(artifact)
            .setRepositories(project.getRemoteProjectRepositories());

        final ArtifactResult result;
        try {
            synchronized (RESOLUTION_LOCK) {
                result =
                    repositorySystem.resolveArtifact(
                        repositorySystemSession,
                        artifactRequest
                    );
            }
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(
                "Error resolving artifact " + NODE_VERSION,
                e
            );
        }

        return result.getArtifact();
    }

    private static final Object RESOLUTION_LOCK = new Object();
}
