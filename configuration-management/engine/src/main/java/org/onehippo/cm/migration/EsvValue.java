/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cm.migration;

public class EsvValue {

    private String value;
    private boolean path;
    private boolean base64;

    public EsvValue(String path) {
        this.path = true;
        this.base64 = false;
        this.value = path;
    }

    public EsvValue(final boolean base64) {
        this.path = false;
        this.base64 = base64;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public boolean isPath() {
        return path;
    }

    public boolean isBase64() {
        return base64;
    }
}
