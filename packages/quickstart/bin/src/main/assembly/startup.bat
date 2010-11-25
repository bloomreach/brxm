@echo off

set ERROR_CODE=0

set INVMODE=0
if NOT x%1 == xINVMODE goto notinvmode
set INVMODE=1
:notinvmode

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of agruments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
%0\
cd %0\..
goto setPaths

:WinNTGetScriptDir
cd /d %0\..
goto setPaths

:setPaths
set BASEDIR=!CD!

if "!REPO!"=="" set REPO="!BASEDIR!\repo"

set EXTRA_JVM_ARGUMENTS=-Xms256m -Xmx256m
goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

if %INVMODE% == 1 goto invmode
cmd /V:ON /S /C startup.bat INVMODE %CMD_LINE_ARGS%
exit /B

:invmode
set INVMODE=0
set CLASSPATH="%BASEDIR%"\lib\classes
for %%j in (lib\*.jar) do set CLASSPATH=%%j;!CLASSPATH!

java %JAVA_OPTS% %EXTRA_JVM_ARGUMENTS% -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Dapp.name="app" -Drepo.path=%REPO% -Dbasedir="%BASEDIR%" org.hippoecm.Main lib\*.war %CMD_LINE_ARGS%
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal

:postExec
exit /B %ERROR_CODE%
