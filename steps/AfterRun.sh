#!/bin/bash

# Java doesn't seem to kill grandchild processes so catch all here.
for i in Standalone mvn byteman tee maven Build skipTests; do
  pkill $i;
done
