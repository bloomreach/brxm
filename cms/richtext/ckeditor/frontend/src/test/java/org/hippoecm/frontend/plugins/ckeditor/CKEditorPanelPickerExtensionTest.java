/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Map;

import org.apache.wicket.behavior.Behavior;
import org.hippoecm.frontend.dialog.DialogBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.images.ImagePicker;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPicker;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.ckeditor.HippoPicker;
import org.onehippo.ckeditor.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link CKEditorPanelPickerExtension}.
 */
public class CKEditorPanelPickerExtensionTest {

    private LinkPicker linkPicker;
    private ImagePicker imagePicker;
    private CKEditorPanelPickerExtension extension;

    @Before
    public void setUp() {
        linkPicker = createMock(LinkPicker.class);
        imagePicker = createMock(ImagePicker.class);
        extension = new CKEditorPanelPickerExtension(linkPicker, imagePicker);
    }

    @Test
    public void callbackUrlsAreAddedToCKEditorConfiguration() throws Exception {
        final String linkPickerCallbackUrl = "./linkpicker/callback";
        final String imagePickerCallbackUrl = "./imagepicker/callback";

        expect(linkPicker.getCallbackUrl()).andReturn(linkPickerCallbackUrl);
        expect(imagePicker.getCallbackUrl()).andReturn(imagePickerCallbackUrl);

        replay(linkPicker, imagePicker);

        final ObjectNode editorConfig = Json.object();
        extension.addConfiguration(editorConfig);

        verify(linkPicker, imagePicker);

        assertTrue("CKEditor config has configuration for the hippopicker plugin", editorConfig.has(HippoPicker.CONFIG_KEY));
        JsonNode hippoPickerConfig = editorConfig.get(HippoPicker.CONFIG_KEY);

        assertTrue("hippopicker config is an object", hippoPickerConfig.isObject());

        JsonNode internalLinkPickerConfig = hippoPickerConfig.get(HippoPicker.InternalLink.CONFIG_KEY);
        assertNotNull("hippopicker configuration has configuration for the internal link picker", internalLinkPickerConfig);
        assertTrue("internal link picker config is an object", internalLinkPickerConfig.isObject());
        assertEquals(linkPickerCallbackUrl, internalLinkPickerConfig.get(HippoPicker.InternalLink.CONFIG_CALLBACK_URL).asText());

        JsonNode imagePickerConfig = hippoPickerConfig.get(HippoPicker.Image.CONFIG_KEY);
        assertNotNull("hippopicker configuration has configuration for the image picker", imagePickerConfig);
        assertTrue("image picker config is an object", imagePickerConfig.isObject());
        assertEquals(imagePickerCallbackUrl, imagePickerConfig.get(HippoPicker.Image.CONFIG_CALLBACK_URL).asText());
    }

    @Test
    public void testGetBehaviors() throws Exception {
        final DialogBehavior linkPickerBehavior = new DialogBehavior() {
            @Override
            protected void showDialog(final Map<String, String> parameters) {}
        };
        final DialogBehavior imagePickerBehavior = new DialogBehavior() {
            @Override
            protected void showDialog(final Map<String, String> parameters) {}
        };

        expect(linkPicker.getBehavior()).andReturn(linkPickerBehavior);
        expect(imagePicker.getBehavior()).andReturn(imagePickerBehavior);
        replay(linkPicker, imagePicker);

        final List<Behavior> behaviors = Lists.newArrayList(extension.getBehaviors());
        assertEquals(2, behaviors.size());
        assertTrue(behaviors.contains(linkPickerBehavior));
        assertTrue(behaviors.contains(imagePickerBehavior));
    }

}
