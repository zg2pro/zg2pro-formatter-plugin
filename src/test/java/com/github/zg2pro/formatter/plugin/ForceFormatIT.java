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

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author zg2pro
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({ "3.8.3" })
public class ForceFormatIT {
    @Rule
    public final TestResources resources = new TestResources(
        "C:\\workspace\\test\\",
        "C:\\workspace\\test2\\"
    );

    public final MavenRuntime verifier;

    public ForceFormatIT(MavenRuntimeBuilder runtimeBuilder) throws Exception {
        this.verifier = runtimeBuilder.build(); //.withCliOptions(opts) // //
    }

    @Test
    public void checkSpringDataExamples() throws Exception {
        File projectDir = new File("C:\\workspace\\test\\project");
        File projectDirTransformed = new File("C:\\workspace\\test2\\project");
        if (projectDirTransformed.exists()) {
            FileUtils.cleanDirectory(projectDirTransformed);
            projectDirTransformed.delete();
        }

        verifier
            .forProject(projectDir) //
            .withCliOption("-B")
            .execute("clean", "compile")
            .assertErrorFreeLog();
    }
}
