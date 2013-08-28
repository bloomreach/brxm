/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.model;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public abstract class RichTextEditorLink extends AbstractPersistedMap {

    private static final long serialVersionUID = 1L;

    public static final String HREF = "f_href";
    public static final String TITLE = "f_title";
    public static final String TARGET = "f_target";
    public static final String TARGET_OPEN_IN_NEW_WINDOW = "_blank";

    public RichTextEditorLink(Map<String, String> values) {
        super(values);
    }

    public String getHref() {
        return get(HREF);
    }

    public void setHref(String href) {
        put(HREF, href);
    }

    public boolean getOpenInNewWindow() {
        final String target = get(TARGET);
        return TARGET_OPEN_IN_NEW_WINDOW.equals(target);
    }

    public void setOpenInNewWindow(boolean isEnabled) {
        if (isEnabled) {
            put(TARGET, TARGET_OPEN_IN_NEW_WINDOW);
        } else {
            put(TARGET, StringUtils.EMPTY);
        }
    }

    @Override
    protected String serializeValue(String value) {
        if (value == null) {
            value = StringUtils.EMPTY;
        }
        return super.serializeValue(value);
    }

}
