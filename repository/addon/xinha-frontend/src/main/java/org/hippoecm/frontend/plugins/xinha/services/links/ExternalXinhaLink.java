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

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.plugins.xinha.dialog.AbstractPersistedMap;

public class ExternalXinhaLink extends AbstractPersistedMap {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String HREF = "f_href";

    private Map<String, String> initialValues;

    public ExternalXinhaLink(Map<String, String> values) {
        super(values);
        initialValues = new HashMap<String, String>(values);
    }

    public String getHref() {
        return (String) get(HREF);
    }

    public void setHref(String href) {
        put(HREF, href);
    }

    public boolean isValid() {
        if (getHref() == null || "".equals(getHref())) {
            return false;
        }
        return true;
    }

    public boolean isExisting() {
        if (getHref() != null && !getHref().equals("")) {
            return true;
        }
        return false;
    }

    public void delete() {
    }

    public void save() {
    }

    public boolean hasChanged() {
        return !equals(initialValues);
    }

}
