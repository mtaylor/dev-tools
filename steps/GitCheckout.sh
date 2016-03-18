#!/bin/bash

/usr/bin/cd $SRC_DIR
git checkout $START_COMMIT
git checkout HEAD~$SEARCH_VALUE