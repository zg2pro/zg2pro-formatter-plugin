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
package com.github.zg2pro.formatter.plugin.util;

import com.github.zg2pro.formatter.plugin.AbstractFormatterService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author zg2pro
 */
public class FileOverwriter extends AbstractFormatterService {
    @Deprecated
    private static final boolean isWindows = System
        .getProperty("os.name")
        .toLowerCase()
        .contains("win");

    public FileOverwriter(Log log) {
        super(log);
    }

    public void checkFileAndOverwriteIfNeedBe(
        File projectFile,
        String filepath,
        Log logger
    )
        throws IOException {
        InputStream ecStream =
            this.getClass().getClassLoader().getResourceAsStream(filepath);
        byte[] targetArray = new byte[ecStream.available()];
        ecStream.read(targetArray);
        String content = new String(targetArray, "UTF-8");
        if (content.contains("end_of_line")) {
            if (isWindows) {
                getLog()
                    .debug(
                        "windows recognized, checking end of lines in editorconfig"
                    );
                content =
                    content.replace("end_of_line = lf", "end_of_line = crlf");
            } else {
                getLog()
                    .debug(
                        "linux recognized, checking end of lines in editorconfig"
                    );
                content =
                    content.replace("end_of_line = crlf", "end_of_line = lf");
            }
        }
        File couldBeExistingFile = projectFile
            .getParentFile()
            .toPath()
            .resolve("." + filepath)
            .toFile();
        if (
            !couldBeExistingFile.exists() ||
            couldBeExistingFile.exists() &&
            !Arrays.equals(
                content.getBytes("UTF-8"),
                Files.readAllBytes(couldBeExistingFile.toPath())
            )
        ) {
            getLog().info("overwrites " + filepath);
            System.out.println("11111:" + couldBeExistingFile);
            System.out.println("aaaaa:" + content);
            FileUtils.writeStringToFile(couldBeExistingFile, content, "UTF-8");
            System.out.println("222222");
        }
    }
}
