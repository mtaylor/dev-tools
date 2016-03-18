#!/bin/bash

cd $WORKSPACE;
rm -rf *journal-*;
for i in `ps ax | grep standalone | grep -o '^\\S\\+'`; do kill -9 $i; done;
./replace-eap.sh $SRC_DIR $WORKSPACE/server1/jboss-eap/
./replace-eap.sh $SRC_DIR $WORKSPACE/server2/jboss-eap/