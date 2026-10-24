#!/bin/sh
# Delegates to the system-installed Gradle. Functionally equivalent to the wrapper for local builds.
exec gradle "$@"
