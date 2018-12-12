/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.deriveddata;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VariantFinderTest extends RepositoryTestCase {

    private String cnd = "<'accessedVariantFinderTest'='http://www.onehippo.org/accessedVariantFinderTest/nt/1.0'>\n" +
            "<'hippo'='http://www.onehippo.org/jcr/hippo/nt/2.0.4'>\n" +
            "<'hippostd'='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n" +
            "\n" +
            "[accessedVariantFinderTest:compound] > hippostd:relaxed \n" +
            "\n" +
            "[accessedVariantFinderTest:basedocument] > hippo:document, hippostd:relaxed, hippostd:document \n" +
            "+ * (accessedVariantFinderTest:compound) \n";
    private Node unpublished;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Reader reader = new InputStreamReader(new ByteArrayInputStream(cnd.getBytes()));
        CndImporter.registerNodeTypes(reader, session);
        unpublished = session.getRootNode().addNode("test", "accessedVariantFinderTest:basedocument");
    }

    @Test
    public void getAccessedVariant_variantIsParentOfProperty() throws RepositoryException {
        unpublished.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        unpublished.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        unpublished.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");
        final Property property = unpublished.setProperty("test", "test");
        VariantFinder finder = new VariantFinder(property);
        assertTrue(finder.find().map(this::isSame).orElse(false));
        session.refresh(false);

    }

    @Test
    public void getAccessedVariant_variantIsGrandFatherOfProperty() throws RepositoryException {
        final Node compound = unpublished.addNode("compound", "accessedVariantFinderTest:compound");
        final Property property = compound.setProperty("test", "test");
        VariantFinder finder = new VariantFinder(property);
        assertTrue(finder.find().map(this::isSame).orElse(false));
        session.refresh(false);
    }

    @Test
    public void getAccessedVariant_isPRopertyOfVariant() throws RepositoryException {
        VariantFinder finder = new VariantFinder(unpublished.setProperty("test", "test"));
        assertTrue(finder.find().map(this::isSame).orElse(false));
        session.refresh(false);
    }


    @Test
    public void getAccessedVariant_propertyIsNotPartOfVariant() throws RepositoryException {
        final Property property = session.getRootNode().setProperty("test", "test");
        VariantFinder finder = new VariantFinder(property);
        assertEquals(Optional.empty(), finder.find());
        session.refresh(false);
    }


    @Test
    public void getUnpublishedVariant() throws RepositoryException{
        unpublished.setProperty(HippoStdNodeType.HIPPOSTD_STATE,"unpublished");
        unpublished.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        unpublished.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        unpublished.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");
        final Node published = session.getRootNode().addNode("test2", "accessedVariantFinderTest:basedocument");
        published.setProperty(HippoStdNodeType.HIPPOSTD_STATE,"published");
        published.setProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY,"live");
        published.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        published.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        published.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");
        published.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, "test");
        final Property property = unpublished.setProperty("test", " test");
        VariantFinder finder = new VariantFinder(property);
        assertTrue(finder.findUnpublished().map(unpublished -> isSame(this.unpublished)).orElse(false));
        session.refresh(false);


    }



    private Boolean isSame(final Node variant) {
        try {
            return variant.isSame(unpublished);
        } catch (RepositoryException e) {
            return false;
        }
    }
}