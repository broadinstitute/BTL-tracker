@REM tracker launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (optional if java on path)
@REM CFG_OPTS  - JVM options (optional)
@REM Configuration:
@REM TRACKER_config.txt found in the TRACKER_HOME.
@setlocal enabledelayedexpansion

@echo off
if "%TRACKER_HOME%"=="" set "TRACKER_HOME=%~dp0\\.."
set ERROR_CODE=0

set "APP_LIB_DIR=%TRACKER_HOME%\lib\"

rem Detect if we were double clicked, although theoretically A user could
rem manually run cmd /c
for %%x in (%cmdcmdline%) do if %%~x==/c set DOUBLECLICKED=1

rem FIRST we load the config file of extra options.
set "CFG_FILE=%TRACKER_HOME%\TRACKER_config.txt"
set CFG_OPTS=
if exist %CFG_FILE% (
  FOR /F "tokens=* eol=# usebackq delims=" %%i IN ("%CFG_FILE%") DO (
    set DO_NOT_REUSE_ME=%%i
    rem ZOMG (Part #2) WE use !! here to delay the expansion of
    rem CFG_OPTS, otherwise it remains "" for this loop.
    set CFG_OPTS=!CFG_OPTS! !DO_NOT_REUSE_ME!
  )
)

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem Detect if this java is ok to use.
for /F %%j in ('"%_JAVACMD%" -version  2^>^&1') do (
  if %%~j==Java set JAVAINSTALLED=1
)

rem BAT has no logical or, so we do it OLD SCHOOL! Oppan Redmond Style
set JAVAOK=true
if not defined JAVAINSTALLED set JAVAOK=false

if "%JAVAOK%"=="false" (
  echo.
  echo A Java JDK is not installed or can't be found.
  if not "%JAVA_HOME%"=="" (
    echo JAVA_HOME = "%JAVA_HOME%"
  )
  echo.
  echo Please go to
  echo   http://www.oracle.com/technetwork/java/javase/downloads/index.html
  echo and download a valid Java JDK and install before running tracker.
  echo.
  echo If you think this message is in error, please check
  echo your environment variables to see if "java.exe" and "javac.exe" are
  echo available via JAVA_HOME or PATH.
  echo.
  if defined DOUBLECLICKED pause
  exit /B 1
)


rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%
if "%_JAVA_OPTS%"=="" set _JAVA_OPTS=%CFG_OPTS%

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
rem "-J" is stripped, "-D" is left as is, and everything is appended to JAVA_OPTS
set _JAVA_PARAMS=

:param_beforeloop
if [%1]==[] goto param_afterloop
set _TEST_PARAM=%~1

rem ignore arguments that do not start with '-'
if not "%_TEST_PARAM:~0,1%"=="-" (
  shift
  goto param_beforeloop
)

set _TEST_PARAM=%~1
if "%_TEST_PARAM:~0,2%"=="-J" (
  rem strip -J prefix
  set _TEST_PARAM=%_TEST_PARAM:~2%
)

if "%_TEST_PARAM:~0,2%"=="-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1-2" %%G in ("%_TEST_PARAM%") DO (
    if not "%%G" == "%_TEST_PARAM%" (
      rem double quoted: "-Dprop=42" -> -Dprop="42"
      set _JAVA_PARAMS=%%G="%%H"
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      set _JAVA_PARAMS=%_TEST_PARAM%=%2
      shift
    )
  )
) else (
  rem a JVM property, we just append it
  set _JAVA_PARAMS=%_TEST_PARAM%
)

:param_loop
shift

if [%1]==[] goto param_afterloop
set _TEST_PARAM=%~1

rem ignore arguments that do not start with '-'
if not "%_TEST_PARAM:~0,1%"=="-" goto param_loop

set _TEST_PARAM=%~1
if "%_TEST_PARAM:~0,2%"=="-J" (
  rem strip -J prefix
  set _TEST_PARAM=%_TEST_PARAM:~2%
)

if "%_TEST_PARAM:~0,2%"=="-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1-2" %%G in ("%_TEST_PARAM%") DO (
    if not "%%G" == "%_TEST_PARAM%" (
      rem double quoted: "-Dprop=42" -> -Dprop="42"
      set _JAVA_PARAMS=%_JAVA_PARAMS% %%G="%%H"
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      set _JAVA_PARAMS=%_JAVA_PARAMS% %_TEST_PARAM%=%2
      shift
    )
  )
) else (
  rem a JVM property, we just append it
  set _JAVA_PARAMS=%_JAVA_PARAMS% %_TEST_PARAM%
)
goto param_loop
:param_afterloop

set _JAVA_OPTS=%_JAVA_OPTS% %_JAVA_PARAMS%
:run
 
set "APP_CLASSPATH=%APP_LIB_DIR%\tracker.tracker-0.1.jar;%APP_LIB_DIR%\org.scala-lang.scala-library-2.11.5.jar;%APP_LIB_DIR%\com.typesafe.play.twirl-api_2.11-1.0.2.jar;%APP_LIB_DIR%\org.apache.commons.commons-lang3-3.1.jar;%APP_LIB_DIR%\com.typesafe.play.play_2.11-2.3.6.jar;%APP_LIB_DIR%\com.typesafe.play.build-link-2.3.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-exceptions-2.3.6.jar;%APP_LIB_DIR%\org.javassist.javassist-3.18.2-GA.jar;%APP_LIB_DIR%\com.typesafe.play.play-iteratees_2.11-2.3.6.jar;%APP_LIB_DIR%\org.scala-stm.scala-stm_2.11-0.7.jar;%APP_LIB_DIR%\com.typesafe.config-1.2.1.jar;%APP_LIB_DIR%\com.typesafe.play.play-json_2.11-2.3.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-functional_2.11-2.3.6.jar;%APP_LIB_DIR%\com.typesafe.play.play-datacommons_2.11-2.3.6.jar;%APP_LIB_DIR%\joda-time.joda-time-2.3.jar;%APP_LIB_DIR%\org.joda.joda-convert-1.6.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-annotations-2.3.2.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-core-2.3.2.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-databind-2.3.2.jar;%APP_LIB_DIR%\io.netty.netty-3.9.3.Final.jar;%APP_LIB_DIR%\com.typesafe.netty.netty-http-pipelining-1.1.2.jar;%APP_LIB_DIR%\org.slf4j.jul-to-slf4j-1.7.6.jar;%APP_LIB_DIR%\org.slf4j.jcl-over-slf4j-1.7.6.jar;%APP_LIB_DIR%\ch.qos.logback.logback-core-1.1.1.jar;%APP_LIB_DIR%\ch.qos.logback.logback-classic-1.1.1.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-slf4j_2.11-2.3.4.jar;%APP_LIB_DIR%\commons-codec.commons-codec-1.9.jar;%APP_LIB_DIR%\xerces.xercesImpl-2.11.0.jar;%APP_LIB_DIR%\xml-apis.xml-apis-1.4.01.jar;%APP_LIB_DIR%\javax.transaction.jta-1.1.jar;%APP_LIB_DIR%\org.reactivemongo.play2-reactivemongo_2.11-0.10.5.0.akka23.jar;%APP_LIB_DIR%\org.reactivemongo.reactivemongo_2.11-0.10.5.0.akka23.jar;%APP_LIB_DIR%\org.reactivemongo.reactivemongo-bson-macros_2.11-0.10.5.0.akka23.jar;%APP_LIB_DIR%\org.scala-lang.scala-compiler-2.11.2.jar;%APP_LIB_DIR%\org.scala-lang.scala-reflect-2.11.2.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-xml_2.11-1.0.2.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-parser-combinators_2.11-1.0.2.jar;%APP_LIB_DIR%\org.reactivemongo.reactivemongo-bson_2.11-0.10.5.0.akka23.jar;%APP_LIB_DIR%\org.apache.logging.log4j.log4j-api-2.0.2.jar;%APP_LIB_DIR%\org.apache.logging.log4j.log4j-core-2.0.2.jar;%APP_LIB_DIR%\org.apache.logging.log4j.log4j-to-slf4j-2.0.2.jar;%APP_LIB_DIR%\org.slf4j.slf4j-api-1.7.7.jar;%APP_LIB_DIR%\org.broadinstitute.sampleracks_2.11-1.9.5.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-actor_2.11-2.3.8.jar;%APP_LIB_DIR%\com.googlecode.concurrentlinkedhashmap.concurrentlinkedhashmap-lru-1.3.2.jar;%APP_LIB_DIR%\io.spray.spray-client_2.11-1.3.2.jar;%APP_LIB_DIR%\io.spray.spray-can_2.11-1.3.2.jar;%APP_LIB_DIR%\io.spray.spray-io_2.11-1.3.2.jar;%APP_LIB_DIR%\io.spray.spray-util_2.11-1.3.2.jar;%APP_LIB_DIR%\io.spray.spray-http_2.11-1.3.2.jar;%APP_LIB_DIR%\org.parboiled.parboiled-scala_2.11-1.1.6.jar;%APP_LIB_DIR%\org.parboiled.parboiled-core-1.1.6.jar;%APP_LIB_DIR%\io.spray.spray-httpx_2.11-1.3.2.jar;%APP_LIB_DIR%\org.jvnet.mimepull.mimepull-1.9.4.jar;%APP_LIB_DIR%\io.spray.spray-routing_2.11-1.3.2.jar;%APP_LIB_DIR%\com.chuusai.shapeless_2.11-1.2.4.jar;%APP_LIB_DIR%\io.spray.spray-json_2.11-1.3.1.jar;%APP_LIB_DIR%\org.apache.poi.poi-3.9.jar;%APP_LIB_DIR%\org.apache.poi.poi-ooxml-3.9.jar;%APP_LIB_DIR%\org.apache.poi.poi-ooxml-schemas-3.9.jar;%APP_LIB_DIR%\org.apache.xmlbeans.xmlbeans-2.3.0.jar;%APP_LIB_DIR%\stax.stax-api-1.0.1.jar;%APP_LIB_DIR%\dom4j.dom4j-1.6.1.jar;%APP_LIB_DIR%\commons-lang.commons-lang-2.6.jar;%APP_LIB_DIR%\org.mongodb.casbah-commons_2.11-2.7.5.jar;%APP_LIB_DIR%\com.github.nscala-time.nscala-time_2.11-1.0.0.jar;%APP_LIB_DIR%\org.mongodb.mongo-java-driver-2.12.5.jar;%APP_LIB_DIR%\org.mongodb.casbah-core_2.11-2.7.5.jar;%APP_LIB_DIR%\org.mongodb.casbah-query_2.11-2.7.5.jar;%APP_LIB_DIR%\org.mongodb.casbah-gridfs_2.11-2.7.5.jar;%APP_LIB_DIR%\org.jfree.jfreechart-1.0.15.jar;%APP_LIB_DIR%\org.jfree.jcommon-1.0.17.jar;%APP_LIB_DIR%\com.lowagie.itext-2.1.5.jar;%APP_LIB_DIR%\bouncycastle.bcmail-jdk14-138.jar;%APP_LIB_DIR%\bouncycastle.bcprov-jdk14-138.jar;%APP_LIB_DIR%\com.sun.mail.javax.mail-1.5.1.jar;%APP_LIB_DIR%\javax.activation.activation-1.1.jar;%APP_LIB_DIR%\org.broadinstitute.spreadsheets_2.11-1.0.3.jar;%APP_LIB_DIR%\org.mapdb.mapdb-1.0.6.jar;%APP_LIB_DIR%\org.broadinstitute.mongo_2.11-1.9.5.jar;%APP_LIB_DIR%\org.broadinstitute.monomersandsequences_2.11-1.9.4.jar;%APP_LIB_DIR%\org.broadinstitute.platesandwells_2.11-1.9.4.jar;%APP_LIB_DIR%\tracker.tracker-0.1-assets.jar"
set "APP_MAIN_CLASS=play.core.server.NettyServer"

rem Call the application and pass all arguments unchanged.
"%_JAVACMD%" %_JAVA_OPTS% %TRACKER_OPTS% -cp "%APP_CLASSPATH%" %APP_MAIN_CLASS% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end

@endlocal

exit /B %ERROR_CODE%
