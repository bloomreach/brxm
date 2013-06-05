/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function AutoResize(editor) {
}

AutoResize._pluginInfo = {
    name         : "AutoResize",
    version      : "1.0",
    developer    : "a.bogaart@1hippo.com",
    developer_url: "http://www.onehippo.com",
    c_owner      : "OneHippo",
    license      : "al2",
    sponsor      : "OneHippo",
    sponsor_url  : "http://www.onehippo.com"
};

//here for backward-compatibility of configurations, doesn't actually do anything anymore
Xinha.Config.prototype.AutoResize = {
    'minHeight' : 150,
    'minWidth'  : 150
};

