Bug Search
===========

Tool for locating the commit that introduced a bug in the code base.  The tool will search back over the git history to
locate the last good commit.  The good commit is defined by run(s) that have not surface the bug.  The bug is defined by
configuring pattern match properties.  These patterns are matched against the std and err from the run.sh step of the
workflow.

Patterns are string (no commas) and can be defined as success or failure.  In other words if pattern is matched the test
is set to passed, or it can be set to failed.  Patterns also have a numner of occurances.  For example, a pattern can
be matched in the log statement, if it occurs X times then a test is set to pass or fail.

In addition the test can be run several times for each commit.  For cases where test results are intermittent.  See the
config files under conf/.

You will need to write your own shell scripts under steps.  For setting running tests and cleaning environment.  Also see
conf/environment.properties for setting up environment variables and patterns.properties for defining patterns.

Usage
------

mvn clean compile
./bin/run.sh

(if running directly from java set DEV_TOOLS_HOME env variable).

Config
-------

Look under conf directory for examples

Steps
------

conf/config.properties defines the workflow steps.  These steps refer to scripts under the steps directory.  The scripts
should be written to suit the test case.