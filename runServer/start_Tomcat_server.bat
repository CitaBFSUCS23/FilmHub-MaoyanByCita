@echo off
cd /d %~dp0..
set CLASSPATH=src/WEB-INF/classes;lib/*
echo Starting Tomcat Server...
echo Classpath: %CLASSPATH%
java -cp "%CLASSPATH%" TomcatStarter
echo Tomcat server started successfully!
pause