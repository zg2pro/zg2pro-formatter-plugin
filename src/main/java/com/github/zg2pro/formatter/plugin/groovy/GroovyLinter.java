/*
 * The MIT License
 *
 * Copyright 2020 ganne.
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

import com.github.zg2pro.formatter.plugin.AbstractFormatterService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.text.BadLocationException;
import org.ec4j.core.ResourceProperties;
import org.ec4j.lint.api.Linter;
import org.ec4j.lint.api.Resource;
import org.ec4j.lint.api.ViolationHandler;
import org.netbeans.modules.editor.indent.IndentSpiPackageAccessor;
import org.netbeans.modules.editor.indent.TaskHandler;
import org.netbeans.modules.groovy.editor.language.GroovyFormatter;
import org.netbeans.modules.groovy.refactoring.GroovyRefactoringFactory;
import org.netbeans.modules.groovy.support.api.GroovySources;
import org.openide.util.Exceptions;

/**
 *
 * @author zg2pro
 */
public class GroovyLinter extends AbstractFormatterService implements Linter {
    private static final List<String> DEFAULT_EXCLUDES = Collections.emptyList();
    private static final List<String> DEFAULT_INCLUDES = Collections.unmodifiableList(
        Arrays.asList("**/*")
    );

    @Override
    public List<String> getDefaultExcludes() {
        return DEFAULT_EXCLUDES;
    }

    @Override
    public List<String> getDefaultIncludes() {
        return DEFAULT_INCLUDES;
    }

    @Override
    public void process(
        Resource arg0,
        ResourceProperties arg1,
        ViolationHandler arg2
    )
        throws IOException {
        MyGroovyFormatter mgf = new MyGroovyFormatter();
        try {
            mgf.reindent(arg0);
        } catch (BadLocationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
