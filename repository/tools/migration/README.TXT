# Copyright 2007 Hippo (www.hippo.nl)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


The migration tool provides a framework for migrating content from a
Hippo Repository 1.2.x (WebDAV) to a Hippo Repository 2 (JCR)


- Running the migration tool

Step 1. Configure

Copy the plugin.properties to local.properties and adjust the Sfile according to your own settings.

Step 2. Run

$ mvn -Dexec.args="local.properties" compile exec:java

- Or..

Step 2. Use Appassembler
$ mvn package appassembler:assemble

Step 3. Run
windows:
$ target\appassembler\bin\migrate.bat <full path to config>
unix:
./target/appassembler/bin/migrate <full path to config>
