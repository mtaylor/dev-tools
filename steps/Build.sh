#!/bin/bash

cd $TEST_DIR
mkdir -p logs/$SEARCH_VALUE

cd $SRC_DIR
mvn clean install -DskipTests | tee $TEST_DIR/logs/$SEARCH_VALUE/build-log