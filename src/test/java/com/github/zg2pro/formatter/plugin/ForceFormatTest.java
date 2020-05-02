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
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author zg2pro
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({ "3.6.3" })
public class ForceFormatTest {
   
    
    @Rule
    public final TestResources resources = new TestResources(
            "src/test/resources/projects", 
            "target/test-classes/projects"
    );
    public final MavenRuntime verifier;
    
    
    public ForceFormatTest(MavenRuntimeBuilder runtimeBuilder) throws Exception {
        this.verifier = runtimeBuilder //
                //.withCliOptions(opts) //
                .build();
    }
    
    @Test
    public void check() throws Exception {

        File projDir = resources.getBasedir("spring-ejb-keynectis-vBefore-format");

        MavenExecution mavenExec = verifier.forProject(projDir) //
                .withCliOption("-X") // debug
                .withCliOption("-B") // batch
        ;
        
        mavenExec //
                .execute("validate") //
               // .assertLogText("[DEBUG] hook placement") //
         ;
    }
    
}
