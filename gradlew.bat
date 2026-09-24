@echo off
setlocal
set DIR=%~dp0
if not defined JAVA_HOME goto findJava
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
goto run
:findJava
where java.exe >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  set JAVA_EXE=java.exe
  goto run
)
echo ERROR: JAVA_HOME is not set and java.exe was not found in PATH.
exit /b 1
:run
"%JAVA_EXE%" -Xmx64m -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
