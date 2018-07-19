#!/usr/bin/env bash

export MAVEN_OPTS="-Xms1G -Xmx1G"
mvn exec:java -Dexec.mainClass="fuhaiwei.bmoe2018.autorun.Rebuild" -Dexec.args="$1"
