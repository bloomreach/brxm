/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.JavaTypeDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({FieldPluginHelper.class})
public class FieldPluginHelperTest {

    private static final String BUNDLE_FIELD_CAPTION_KEY_VALUE = "Bundle Field Caption Key Value";
    private static final String BUNDLE_CAPTION_KEY_VALUE = "Bundle Caption Key Value";
    private static final String DEFAULT_CONFIGURED_CAPTION = "Default Configured Caption Value";
    private static final String DEFAULT_PARAMETER_CAPTION = "Default Parameter Caption Value";

    private PluginPage page;

    @Before
    public void setUp(){
        final HippoTester tester = new HippoTester();
        page = (PluginPage) tester.startPluginPage();
    }

    @Test
    public void testGetCaptionModelWithoutField() {

        final IPluginConfig pluginConfig = new JavaPluginConfig();
        final FieldPluginHelper helper = new NoServiceFieldPluginHelper(null, pluginConfig);

        // no field, no config: undefined
        IModel<String> caption = helper.getCaptionModel(page.getComponent());
        assertEquals("undefined", caption.getObject());

        // set caption, without method parameter: will read from config
        pluginConfig.put("caption", DEFAULT_CONFIGURED_CAPTION);
        caption = helper.getCaptionModel(page.getComponent());
        assertEquals(DEFAULT_CONFIGURED_CAPTION, caption.getObject());

        // with method parameter: will read from it
        caption = helper.getCaptionModel(page.getComponent(), DEFAULT_PARAMETER_CAPTION);
        assertEquals(DEFAULT_PARAMETER_CAPTION, caption.getObject());

        // add key: will read from bundle
        pluginConfig.put("captionKey", "configuredCaptionKey");
        caption = helper.getCaptionModel(page.getComponent(), DEFAULT_PARAMETER_CAPTION);
        assertEquals(BUNDLE_CAPTION_KEY_VALUE, caption.getObject());

        // set key without bundle value: will fallback to default caption
        pluginConfig.put("captionKey", "nonExistingCaptionKey");
        caption = helper.getCaptionModel(page.getComponent());
        assertEquals(DEFAULT_CONFIGURED_CAPTION, caption.getObject());

        // set key without bundle value, without default caption: will capitalize captionKey
        pluginConfig.remove("caption");
        caption = helper.getCaptionModel(page.getComponent());
        assertEquals("NonExistingCaptionKey", caption.getObject());
    }

    @Test
    public void testGetCaptionModelWithFieldFromBundle() {
        final IPluginConfig pluginConfig = new JavaPluginConfig();
        pluginConfig.put(AbstractFieldPlugin.FIELD, "testfield");

        final FieldPluginHelper helper = new NoServiceFieldPluginHelper(null, pluginConfig);
        final JavaFieldDescriptor field = new JavaFieldDescriptor("testns", new JavaTypeDescriptor("testfield", "String", null));
        Whitebox.setInternalState(helper, field);

        // caption taken from bundle base on field path
        final IModel<String> caption = helper.getCaptionModel(page.getComponent());
        assertEquals("Bundle Field Caption Key Value", caption.getObject());
    }

    @Test
    public void testGetCaptionModelWithFieldNoBundle() {
        final IPluginConfig pluginConfig = new JavaPluginConfig();
        pluginConfig.put(AbstractFieldPlugin.FIELD, "testFieldNoBundle");

        final FieldPluginHelper helper = new NoServiceFieldPluginHelper(null, pluginConfig);
        final JavaFieldDescriptor field = new JavaFieldDescriptor("testns", new JavaTypeDescriptor("testFieldNoBundle","String", null));
        Whitebox.setInternalState(helper, field);

        // no config: capitalize lowercase field name
        IModel<String> caption = helper.getCaptionModel(page.getComponent());
        assertEquals("Testfieldnobundle", caption.getObject());

        // with config: will read default caption
        pluginConfig.put("caption", DEFAULT_CONFIGURED_CAPTION);
        caption = helper.getCaptionModel(page.getComponent());
        assertEquals(DEFAULT_CONFIGURED_CAPTION, caption.getObject());
    }

    /**
     * FieldPluginHelper that does not use services
     */
    private class NoServiceFieldPluginHelper extends FieldPluginHelper {
        NoServiceFieldPluginHelper(final IPluginContext context, final IPluginConfig pluginConfig) {
            super(context, pluginConfig);
        }

        @Override
        public ITemplateEngine getTemplateEngine() {
            // no engine needed for the getCaptionModel method
            return null;
        }

        @Override
        String getStringFromBundle(final String key) {
            if ("configuredCaptionKey".equals(key)) {
                return BUNDLE_CAPTION_KEY_VALUE;
            } else if ("testns:testfield".equals(key)) {
                return BUNDLE_FIELD_CAPTION_KEY_VALUE;
            }

            return null;
        }
    }
}