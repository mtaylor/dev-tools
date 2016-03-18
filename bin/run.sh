#!/bin/bash

DEV_TOOLS_HOME=$(dirname $(cd "$(dirname "$BASH_SOURCE")"; pwd))
cd $DEV_TOOLS_HOME
mvn clean compile
java -cp target/classes/ com.amq.dev.tools.BugSearch
