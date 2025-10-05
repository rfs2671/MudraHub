#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

APP_HOME=$(dirname "$0")
APP_HOME=$(cd "$APP_HOME" && pwd)

DEFAULT_JVM_OPTS=""
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Add default JVM options here
JAVA_OPTS=""

exec java $DEFAULT_JVM_OPTS $JAVA_OPTS -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
