package com.github.zg2pro.formatter.zg2pro.formatter.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(defaultPhase = LifecyclePhase.VALIDATE, name = "apply", threadSafe = true)
public class ForceFormatMojo extends AbstractMojo {

    @Component
    public MavenProject project;

    @Component
    public MavenSession session;
    @Component
    public BuildPluginManager pluginManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        executeMojo(
                plugin(
                        groupId("com.hubspot.maven.plugins"),
                        artifactId("prettier-maven-plugin"),
                        version("0.5")
                ),
                goal("write"),
                configuration(
                        
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }

}
