/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.easymock.EasyMock;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.junit.Test;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.AbstractTaxonomyTest;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;

public class JcrCategoryTest extends AbstractTaxonomyTest {

    /**
     * Tests is for the functionality of the LazyMap. Now locale resolving is introduced, the LazyMap cannot be used
     * anymore. If the now deprecated code will be removed, this test can be removed also.
     */
    @Test
    public void testGetInfosReturningLazyMap() throws Exception {
        Node node = EasyMock.createNiceMock(Node.class);
        EasyMock.expect(node.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)).andReturn(true).anyTimes();
        EasyMock.expect(node.getName()).andReturn("testNode").anyTimes();
        EasyMock.replay(node);
        IModel<Node> nodeModel = new JcrNodeModel(node);

        final EditableCategoryInfo categoryInfoEn = EasyMock.createNiceMock(EditableCategoryInfo.class);
        final EditableCategoryInfo categoryInfoFr = EasyMock.createNiceMock(EditableCategoryInfo.class);
        EasyMock.replay(categoryInfoEn);
        EasyMock.replay(categoryInfoFr);

        JcrCategory category = new JcrCategory(nodeModel, true, getService()) {
            @Override
            public EditableCategoryInfo getInfo(final String language) {
                if ("en".equals(language)) {
                    return categoryInfoEn;
                } else if ("fr".equals(language)) {
                    return categoryInfoFr;
                }

                return null;
            }
        };

        assertEquals(categoryInfoEn, category.getInfo("en"));
        assertEquals(categoryInfoFr, category.getInfo("fr"));

        Map<String, ? extends CategoryInfo> infos = category.getInfos();
        assertNotNull(infos);

        assertEquals(categoryInfoEn, infos.get("en"));
        assertEquals(categoryInfoFr, infos.get("fr"));
    }
}
