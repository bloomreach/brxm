/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.dialog;

import org.apache.wicket.model.Model;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EscapeHtmlStringModelTest {

    @Test
    public void escapeTags() {
        EscapeHtmlStringModel model = new EscapeHtmlStringModel(Model.of("<div>html</div>"));
        assertEquals("&lt;div&gt;html&lt;/div&gt;", model.getObject());
    }

    @Test
    public void escapeDoubleQuotes() {
        EscapeHtmlStringModel model = new EscapeHtmlStringModel(Model.of("\"quoted text\""));
        assertEquals("&quot;quoted text&quot;", model.getObject());
    }

    @Test
    public void escapeAmpersand() {
        EscapeHtmlStringModel model = new EscapeHtmlStringModel(Model.of("Hippo & Co"));
        assertEquals("Hippo &amp; Co", model.getObject());
    }

    @Test
    public void nullDelegate() {
        EscapeHtmlStringModel model = new EscapeHtmlStringModel(null);
        assertNull(model.getObject());
    }

    @Test
    public void nullString() {
        EscapeHtmlStringModel model = new EscapeHtmlStringModel(Model.of((String)null));
        assertNull(model.getObject());
    }

    @Test
    public void emptyString() {
        EscapeHtmlStringModel model = new EscapeHtmlStringModel(Model.of(""));
        assertEquals("", model.getObject());
    }

}