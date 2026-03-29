@ECHO OFF
SETLOCAL

SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_HOME=%DIRNAME%

SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
IF NOT EXIST "%JAVA_EXE%" SET JAVA_EXE=java.exe

"%JAVA_EXE%" -Xmx64m -Xms64m -cp "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*