[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-Ready--to--Code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/zg2pro/zg2pro-formatter-plugin) 
[![Build](https://travis-ci.org/zg2pro/zg2pro-formatter-plugin.svg?branch=master)](https://travis-ci.org/zg2pro/zg2pro-formatter-plugin)
[![BCH compliance](https://bettercodehub.com/edge/badge/zg2pro/zg2pro-formatter-plugin?branch=master)](https://bettercodehub.com/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.zg2pro/zg2pro-formatter-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.zg2pro/zg2pro-formatter-plugin)

# zg2pro-formatter-plugin

## Presentation

Formats your java code during the validate phase of your maven lifecycle

To skip the execution of the plugin, either put a <skip>true</skip> tag in config or run your "mvn install" command with -Dzg2pro.format.skip=true

## Basic usage

`zg2pro-formatter-plugin` requires Java 8+.

To install it in your project your project:

[source,xml]
----
  <plugin>
    <groupId>com.github.zg2pro.formatter</groupId>
    <artifactId>zg2pro-formatter-plugin</artifactId>
    <version><!-- check the latest on http://central.maven.org/maven2/com/github/zg2pro/zg2pro-formatter-plugin/ --></version>
    <executions>
      <execution>
        <goals>
          <goal>apply</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
----

This should be put only in the root pom.xml

## How it works

At each execution, zg2pro-formatter-plugin goes through 1 preparation + 3 phases

The preparation only takes place when building the root project of your multi-modules project. It will still be run in a single module project. This preparation consists of writing a standardized .editorconfig file to your root directory, as well as writing a standardized .hooks/pre-commit file. If you want to use a different editorconfig or if you want to use your own pre-commit hooks, this plugin is not for you.

Then the first step, configures your git with the pre-commit script. This will check your code format everytime before you can commit your code with git. Each time, during this pre-commit, it will run the formatting. As the plugin formats the code by itself, if it finds unformatted code, you can restart your commit without any manual change.

The second step is the prettier java phase. Prettier uses prettier-java to format all your java code. This plugin bases on the indent-style set in the .editorconfig file. It will format .java files only.

Then, last but not least, editorconfig-java runs. It scans all the files which are not ignored by your .gitignore file, for each file selected, if this file is not binary data, it will run the editorconfig TextLinter and XmlLinter. Also it will check your files encoding.

When the plugin is inserted in your root pom.xml, you just run once a maven build (minimal phase= validate) and all will be setup.

## License

This library is developed under MIT License

## Continuous integration and deployment to central maven

Build and tests performed by travis CI

Major improvements are git-tagged and pushed to oss.sonatype/maven2 at the same time.
