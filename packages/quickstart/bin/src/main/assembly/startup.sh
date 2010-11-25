#!/bin/sh

cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_VERSION" ] ; then
             JAVA_VERSION="CurrentJDK"
           else
             echo "Using Java version: $JAVA_VERSION"
           fi
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/${JAVA_VERSION}/Home
           fi
           ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

if $cygwin ; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# If a specific java binary isn't specified search for the standard 'java' binary
if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=java
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  Unable to execute execute $JAVACMD"
  exit 1
fi

BASEDIR=`dirname $0`
BASEDIR=`(cd "$BASEDIR"; pwd)`
BASEDIR=`cd "$BASEDIR" && pwd`
cd "$BASEDIR"

for jar in lib/*.jar ; do
  jars=$jars:$jar
done
if [ -z "$CLASSPATH" ]; then
  CLASSPATH="lib/classes$jars"
else
  CLASSPATH="lib/classes$jars:$CLASSPATH"
fi

EXTRA_JVM_ARGUMENTS="-Xms256m -Xmx256m"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  [ -n "$HOME" ] &&
    HOME=`cygpath --path --windows "$HOME"`
fi

exec "$JAVACMD" $JAVA_OPTS $EXTRA_JVM_ARGUMENTS -classpath "$CLASSPATH" org.hippoecm.Main lib/*.war "$@"
