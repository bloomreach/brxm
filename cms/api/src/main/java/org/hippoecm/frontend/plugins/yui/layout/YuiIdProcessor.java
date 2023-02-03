/*
 *  Copyright 2010-2023 Bloomreach
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
package org.hippoecm.frontend.plugins.yui.layout;

import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonValueProcessor;

public class YuiIdProcessor implements JsonValueProcessor {

    public Object processArrayValue(Object value, JsonConfig jsonConfig) {
        return null;
    }

    public Object processObjectValue(String key, Object value, JsonConfig jsonConfig) {
        YuiId id = (YuiId) value;
        String elid = id.getElementId();
        if (elid != null && !"".equals(elid)) {
            return elid;
        }
        return null;
    }

}
