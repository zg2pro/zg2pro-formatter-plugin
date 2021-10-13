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

import static java.util.Collections.singleton;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author zg2pro
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({ "3.8.3" })
public class ForceFormatTest {
    @Rule
    public final TestResources resources = new TestResources(
        "target/test-classes/projects",
        "target/test-classes/transformed-projects"
    );

    public final MavenRuntime verifier;

    public ForceFormatTest(MavenRuntimeBuilder runtimeBuilder)
        throws Exception {
        this.verifier = runtimeBuilder.build(); //.withCliOptions(opts) // //
    }

    @Test
    public void checkSpringMvcKeynectis() throws Exception {
        testPluginByGithubZg2proProject(
            "springmvc-ejb-keynectis",
            "before-formats"
        );
    }

    @Test
    public void checkSnail() throws Exception {
        testPluginByGithubZg2proProject("snail", "master");
    }

    //    @Test
    //    public void checkSpringDataExamples() throws Exception {
    //        testPluginByGithubZg2proProject("spring-data-examples", "master");
    //    }

    private void testPluginByGithubZg2proProject(
        String projectName,
        String branchName
    )
        throws GitAPIException, IOException, Exception {
        File projectDir = new File(
            "target/test-classes/projects/" + projectName
        );
        if (projectDir.exists()) {
            FileUtils.cleanDirectory(projectDir);
            projectDir.delete();
        }
        File projectDirTransformed = new File(
            "target/test-classes/transformed-projects/" + projectName
        );
        if (projectDirTransformed.exists()) {
            FileUtils.cleanDirectory(projectDirTransformed);
            projectDirTransformed.delete();
        }

        Git
            .cloneRepository()
            .setURI("https://github.com/zg2pro/" + projectName + ".git")
            .setDirectory(projectDir)
            .setBranchesToClone(singleton("refs/heads/" + branchName))
            .setBranch("refs/heads/" + branchName)
            .call();

        File projDir = resources.getBasedir(projectName);

        verifier
            .forProject(projDir) //
            // .withCliOption("-X") // debug
            .withCliOption("-B")
            .execute(
                "clean",
                "validate",
                "com.github.zg2pro.formatter:zg2pro-formatter-plugin:0.10-SNAPSHOT:apply"
            )
            .assertErrorFreeLog();
    }
}
