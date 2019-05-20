#!/usr/bin/env bash

set -e

if ! id -g $devuid >/dev/null 2>&1; then
	addgroup --gid $devgid $devusername
	adduser --gid $devgid --uid $devuid $devusername
	chown -R $devuid:$devgid /usr/local/tomcat /brxm
fi
