/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.yui.javascript;

public class StringArrayValue extends Value<String[]> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean escaped;

    public StringArrayValue(String[] value, boolean escaped) {
        super(value);
        this.escaped = escaped;
    }

    public String getScriptValue() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (value != null) {
            for (int i = 0; i < value.length; i++) {
                if (i > 0) {
                    buf.append(',');
                }
                if (escaped) {
                    buf.append(StringValue.escapeString(value[i]));
                } else {
                    buf.append(value[i]);
                }
            }
        }
        buf.append(']');
        return buf.toString();
    }
}
