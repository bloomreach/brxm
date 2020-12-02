/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.apache.wicket.util.tester.FormTester;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;
import org.onehippo.taxonomy.plugin.model.ClassificationDao;
import org.onehippo.taxonomy.plugin.model.JcrTaxonomy;

import static org.junit.Assert.assertEquals;

public class TaxonomyPickerPluginTest extends AbstractTaxonomyTest  {

    static class TaxonomyService implements ITaxonomyService {

        public List<String> getTaxonomies() {
            List<String> list = new ArrayList<>(1);
            list.add("taxonomy");
            return list;
        }

        public Taxonomy getTaxonomy(String name) {
            return new JcrTaxonomy(new JcrNodeModel("/test/taxonomy"), false, this);
        }

        public Taxonomy getTaxonomy(Node taxonomyDocumentNode) {
            return new JcrTaxonomy(new JcrNodeModel("/test/taxonomy"), false, this);
        }

        @Override
        public List<JcrCategoryFilter> getCategoryFilters() {
            return Collections.emptyList();
        }
    }
    final static String[] content = {
            "/test/plugin", "frontend:pluginconfig",
                "plugin.class", TaxonomyPickerPlugin.class.getName(),
                "wicket.id", "service.root",
                "wicket.model", "service.model",
                "taxonomy.name", "taxonomy",
                ClassificationDao.SERVICE_ID, "service.classification.dao",
                "mode", "edit",
            "/test/dao", "frontend:pluginconfig",
                "plugin.class", MixinClassificationDaoPlugin.class.getName(),
                ClassificationDao.SERVICE_ID, "service.classification.dao",
            "/test/content", "nt:unstructured",
    };

    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);

        Node node = (Node) session.getItem("/test/content");
        node.addMixin(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CLASSIFIABLE);
        node.setProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS, new String[] { BRANCH_KEY });
        node.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        node.setProperty(HippoTranslationNodeType.LOCALE, "en");
        node.setProperty(HippoTranslationNodeType.ID, "fake-id");
        session.save();

        context.registerService(new TaxonomyService(), ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID);

        ModelReference ref = new ModelReference("service.model", new JcrNodeModel("/test/content"));
        ref.init(context);

        start(new JcrPluginConfig(new JcrNodeModel("/test/dao")));

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));

    }

    @Test
    @Ignore
    public void testPickerDialog() throws Exception {
        start(config);

        // The restyled tree adds a head contribution (treehelper.js). In order to make the test pass, the page
        // therefore needs to have a YUI Manager behavior, which is what we do here.
        home.add(new WebAppBehavior(new WebAppSettings()));

        // open dialog
        tester.clickLink("root:edit:dialog-link");

        // select top
        tester.clickLink("dialog:content:form:content:tree:i:1:nodeLink");

        // add top to classification
        tester.clickLink("dialog:content:form:content:container:details:add");

        // submit form
        // FIXME: this should really be an ajax submit
        FormTester formTester = tester.newFormTester("dialog:content:form");
        formTester.submit();

        tester.dumpPage();
        // there should be two labels now
        tester.assertLabel("root:keys:1:key", "branch-name-en");
        tester.assertLabel("root:keys:2:key", "top");

        Value[] values = ((Property) session.getItem("/test/content/" + TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS))
                .getValues();
        assertEquals(2, values.length);
        assertEquals(BRANCH_KEY, values[0].getString());
        assertEquals(TOP_KEY, values[1].getString());

    }

}
