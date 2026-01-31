@echo off
echo Compiling all Java files...

cd /d %~dp0..

REM Set classpath to include all jar files in lib directory
set CLASSPATH=lib\*

echo.
echo Compiling all Java files in src/WEB-INF/java directory...

REM Compile all Java files
javac -cp "%CLASSPATH%" -d src/WEB-INF/classes -sourcepath src/WEB-INF/java -encoding UTF-8 src/WEB-INF/java/*.java

if errorlevel 1 (
    echo Compilation failed!
) else (
    echo Compilation succeeded!
)

echo.
echo Checking compiled class files...
dir "src/WEB-INF/classes" /s /b

echo.
echo Compilation complete!
pause