#!/bin/bash

cd $TEST_DIR
mkdir -p logs/$SEARCH_VALUE/runs
mvn clean test -Dtest=ReplicatedColocatedClusterFailoverTestCase#testFailbackClientAckTopic -DfailIfNoTests=false -Deap=7x | tee logs/$SEARCH_VALUE/runs/log-$ITERATION