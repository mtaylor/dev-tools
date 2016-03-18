#!/bin/bash

cd $TEST_DIR
mvn clean test -Dtest=ReplicatedColocatedClusterFailoverTestCase#testFailbackClientAckTopic -DfailIfNoTests=false -Deap=7x | tee log