#!/usr/bin/env bash

set -e

if ! id -g $devgid >/dev/null 2>&1; then
	addgroup --gid $devgid $devusername
	chown -R :$devgid /usr/local/tomcat /brxm
fi

if ! id -u $devuid >/dev/null 2>&1; then
	adduser --gid $devgid --uid $devuid $devusername
	chown -R $devuid /usr/local/tomcat /brxm
fi
