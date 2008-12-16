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
package org.hippoecm.frontend.plugins.xinha.dialog.links;

import java.util.Map;

import org.hippoecm.frontend.plugins.xinha.dialog.JsBean;

public class XinhaLink extends JsBean {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String HREF = "f_href";
    public static final String TITLE = "f_title";
    public static final String TARGET = "f_target";
    public static final String OTHER_TARGET = "f_other_target";

    public XinhaLink(Map<String, String> values) {
        super(values);
    }
    
    public String getHref() {
        return values.get(HREF);
    }

    public void setHref(String href) {
        values.put(HREF, href);
    }
    
}