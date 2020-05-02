[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-Ready--to--Code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/zg2pro/zg2pro-formatter-plugin) 
[![Build](https://travis-ci.org/zg2pro/zg2pro-formatter-plugin.svg?branch=master)](https://travis-ci.org/zg2pro/zg2pro-formatter-plugin)
[![BCH compliance](https://bettercodehub.com/edge/badge/zg2pro/zg2pro-formatter-plugin?branch=master)](https://bettercodehub.com/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.zg2pro/zg2pro-formatter-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.zg2pro/zg2pro-formatter-plugin)

# zg2pro-formatter-plugin

## Presentation

Formats your java code during the validate phase of your maven lifecycle

To skip the execution of the plugin, either put a <skip>true</skip> tag in config or run your "mvn install" command with -Dzg2pro.format.skip=true

## License

This library is developed under MIT License

## Continuous integration and deployment to central maven

Build and tests performed by travis CI

Major improvements are git-tagged and pushed to oss.sonatype/maven2 at the same time.
