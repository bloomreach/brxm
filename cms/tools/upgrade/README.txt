# Copyright 2008 Hippo (www.onehippo.com)
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

# create app
mvn clean install

# run
sh target/canonicalsv/bin/canonicalsv [-p <jcr prefix path>] <input.xml> > <output file>
# or
cat <input.xml> | sh target/canonicalsv/bin/canonicalsv  [-p <jcr prefix path>] > <output file>

# get help:
sh target/canonicalsv/bin/canonicalsv -h