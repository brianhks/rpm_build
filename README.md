# rpm_build
Tablesaw build script for creating an RPM for any java program.

To build run this command to setup your CLASSPATH for tablesaw
> export CLASSPATH=tools/`ls -1 tools | grep tablesaw`

Then run
> java make

To clean things up run
> java make clean
