/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.ckeditor.Json;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests {@link org.hippoecm.frontend.plugins.ckeditor.CKEditorPanel}.
 */
public class CKEditorPanelTest {

    private IHeaderResponse headerResponse;
    private ObjectNode config;

    @Before
    public void setUp() {
        headerResponse = EasyMock.createMock(IHeaderResponse.class);
        config = Json.object();
    }

    @Test
    public void renderSingleContentsCss() {
        config.put(CKEditorConfig.CONTENTS_CSS, "ckeditor/hippocontents.css");

        expectCssHeaderItemRendered("ckeditor/hippocontents.css");

        replay(headerResponse);
        CKEditorPanel.renderContentsCss(headerResponse, config);
        verify(headerResponse);
    }

    @Test
    public void renderMultipleContentsCss() {
        List<String> files = Arrays.asList("ckeditor/hippocontents.css", "extra.css");
        ArrayNode array = Json.array();
        for (String file : files) {
            array.add(file);
        }

        config.set(CKEditorConfig.CONTENTS_CSS, array);

        expectCssHeaderItemRendered("ckeditor/hippocontents.css");
        expectCssHeaderItemRendered("extra.css");

        replay(headerResponse);
        CKEditorPanel.renderContentsCss(headerResponse, config);
        verify(headerResponse);
    }

    private void expectCssHeaderItemRendered(String url) {
        final CssUrlReferenceHeaderItem headerItem = CssHeaderItem.forUrl(url);
        headerResponse.render(eq(headerItem));
        expectLastCall();
    }
}
