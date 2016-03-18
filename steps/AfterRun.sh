#!/bin/bash

cd $WORKING_DIR
for i in `ps ax | grep standalone | grep -o '^\\S\\+'`; do kill -9 $i; done;