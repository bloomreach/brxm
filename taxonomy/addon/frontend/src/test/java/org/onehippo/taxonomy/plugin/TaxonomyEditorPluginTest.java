/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import static org.junit.Assert.assertNotNull;

import org.apache.wicket.util.tester.FormTester;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.junit.Before;
import org.junit.Test;

public class TaxonomyEditorPluginTest extends AbstractTaxonomyTest  {

    final static String[] content = {
            "/test/plugin", "frontend:pluginconfig",
                "plugin.class", TaxonomyEditorPlugin.class.getName(),
                "wicket.id", "service.root",
                "wicket.model", "service.model",
                "mode", "edit",
            "/test/content", "nt:unstructured",
    };

    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);

        ModelReference ref = new ModelReference("service.model", new JcrNodeModel("/test/taxonomy"));
        ref.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    @Test
    public void testNewCategoryDialog() throws Exception {
        start(config);

        // The restyled tree adds a head contribution (treehelper.js). In order to make the test pass, the page
        // therefore needs to have a YUI Manager behavior, which is what we do here.
        home.add(new WebAppBehavior(new WebAppSettings()));

        tester.startPage(home);

        // select top
        tester.clickLink("root:tree:i:1:nodeLink");

        tester.clickLink("root:toolbar-container-holder:add-category");

        FormTester formTester = tester.newFormTester("dialog:content:form");
        formTester.setValue("name", "test-category");
        formTester.submit();

        tester.assertLabel("root:tree:i:8:nodeLink:label", "test-category");
        assertNotNull(taxonomy.getCategoryByKey("test-category"));
    }
}
