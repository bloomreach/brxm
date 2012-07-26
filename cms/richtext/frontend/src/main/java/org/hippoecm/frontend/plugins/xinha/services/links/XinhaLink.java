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
package org.hippoecm.frontend.plugins.xinha.services.links;

import java.util.Map;

import org.hippoecm.frontend.plugins.xinha.model.AbstractPersistedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaLink extends AbstractPersistedMap {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(XinhaLink.class);

    public static final String NEW_WINDOW = "_blank";

    public static final String HREF = "f_href";
    public static final String TITLE = "f_title";
    public static final String TARGET = "f_target";
    public static final String OTHER_TARGET = "f_other_target";

    public XinhaLink(Map<String, String> values) {
        super(values);
    }

    public String getHref() {
        return get(HREF);
    }

    public void setHref(String href) {
        put(HREF, href);
    }

    public boolean getTarget() {
        String value = get(TARGET);
        if (value != null && value.equals(NEW_WINDOW)) {
            return true;
        }
        return false;
    }

    public void setTarget(boolean object) {
        if (object) {
            put(TARGET, NEW_WINDOW);
        } else {
            put(TARGET, "");
        }
    }

    @Override
    protected Object serializeValue(Object value) {
        if (value == null) {
            value = "";
        }
        return super.serializeValue(value);
    }

}
