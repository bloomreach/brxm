/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.ckeditor.CKEditorConstants;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link org.hippoecm.frontend.plugins.ckeditor.CKEditorPanel}.
 */
public class CKEditorPanelTest {

    private IHeaderResponse headerResponse;
    private JSONObject config;

    @Before
    public void setUp() {
        headerResponse = EasyMock.createMock(IHeaderResponse.class);
        config = new JSONObject();
    }

    @Test
    public void renderSingleContentsCss() throws JSONException {
        config.put(CKEditorConstants.CONFIG_CONTENTS_CSS, "ckeditor/hippocontents.css");

        expectCssHeaderItemRendered("ckeditor/hippocontents.css");

        replay(headerResponse);
        CKEditorPanel.renderContentsCss(headerResponse, config);
        verify(headerResponse);
    }

    @Test
    public void renderMultipleContentsCss() throws JSONException {
        List<String> files = Arrays.asList("ckeditor/hippocontents.css", "extra.css");
        config.put(CKEditorConstants.CONFIG_CONTENTS_CSS, new JSONArray(files));

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
