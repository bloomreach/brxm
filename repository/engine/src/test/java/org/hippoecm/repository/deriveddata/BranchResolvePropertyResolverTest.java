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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Assert;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class BranchResolvePropertyResolverTest extends RepositoryTestCase {

    private String cnd = "<'accessedVariantFinderTest'='http://www.onehippo.org/accessedVariantFinderTest/nt/1.0'>\n" +
            "<'hippo'='http://www.onehippo.org/jcr/hippo/nt/2.0.4'>\n" +
            "<'hippostd'='http://www.onehippo.org/jcr/hippostd/nt/2.0'>\n" +
            "\n" +
            "[accessedVariantFinderTest:compound] > hippostd:relaxed \n" +
            "\n" +
            "[accessedVariantFinderTest:basedocument] > hippo:document, hippostd:relaxed, hippostd:document \n" +
            "+ * (accessedVariantFinderTest:compound) \n";
    private Node derived;
    private Node accessed;
    private BranchResolvePropertyResolver resolver;
    private Property property;
    private PropertyResolver defaultPropertyResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Reader reader = new InputStreamReader(new ByteArrayInputStream(cnd.getBytes()));
        CndImporter.registerNodeTypes(reader, session);

        final Node contentRoot = session.getRootNode().addNode("content", NodeType.NT_UNSTRUCTURED);

        derived = contentRoot .addNode("derived", "accessedVariantFinderTest:basedocument");
        derived.setProperty("hippostd:content", "hippostd:content");
        derived.setProperty("hippostd:stateSummary", "new");
        derived.setProperty("hippostd:state", "unpublished");
        accessed = contentRoot .addNode("accessed", "accessedVariantFinderTest:basedocument");
        accessed.setProperty("hippostd:content", "hippostd:content");
        accessed.setProperty("hippostd:stateSummary", "new");
        accessed.setProperty("hippostd:state", "unpublished");
        accessed.addMixin("mix:versionable");
        property = accessed.setProperty("test", "testB");
        defaultPropertyResolver = new PropertyResolver() {
            @Override
            public Property getProperty() {
                return property;
            }

            @Override
            public String getRelativePath() {
                return "test";
            }

            @Override
            public Node getModified() {
                return derived;
            }

            @Override
            public void resolve() {
                // nothing to resolve
            }


        };

    }

    @Override
    public void tearDown() throws Exception {
        if (session.nodeExists("/content")) {
            session.getNode("/content").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void getProperty() throws RepositoryException {
        accessed.setProperty("test", "test");
        Assert.assertEquals(property, BranchResolvePropertyResolver.getProperty(defaultPropertyResolver));
    }

    @Test
    public void getProperty_BranchIdsDontMatch() throws RepositoryException {
        derived.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        derived.setProperty(HippoStdNodeType.HIPPOSTD_STATE, UNPUBLISHED);
        derived.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        derived.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");
        session.save();

        accessed.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");
        accessed.setProperty("test", "testA");

        session.save();

        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        final Version accessedB = versionManager.checkpoint("/content/accessed");
        accessedB.getContainingHistory().addVersionLabel("1.0", "A-unpublished", true);
        session.save();

        property = accessed.setProperty("test", "testB");
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "B");

        final String expected = accessedB.getFrozenNode().getProperty("test").getValue().getString();
        final String actual = BranchResolvePropertyResolver.getProperty(defaultPropertyResolver).getValue().getString();
        assertEquals(expected, actual);
    }


    @Test(expected = RepositoryException.class)
    public void getProperty_BranchIdsDontMatchNoProperVersion() throws RepositoryException {
        derived.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        derived.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        derived.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");

        accessed.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "B");
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "B");

        BranchResolvePropertyResolver.getProperty(defaultPropertyResolver);
    }

    @Test
    public void getProperty_AccessedPropertyIsDescendantOfPublishedVariant() throws RepositoryException {

        derived.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        derived.setProperty(HippoStdNodeType.HIPPOSTD_STATE, UNPUBLISHED);
        derived.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "A");
        derived.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "A");

        accessed.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "B");
        accessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "B");
        accessed.setProperty("test", "testAFromVersionHistory");
        session.save();

        final Node publishedAccessed = accessed.getParent().addNode(accessed.getName(), accessed.getPrimaryNodeType().getName());
        JcrUtils.copyTo(accessed, publishedAccessed);
        publishedAccessed.setProperty("hippostd:state", PUBLISHED);
        publishedAccessed.addMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
        publishedAccessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "B");
        publishedAccessed.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME, "B");
        session.save();

        final VersionManager versionManager = session.getWorkspace().getVersionManager();
        versionManager.checkpoint(accessed.getPath());
        versionManager.getVersionHistory(accessed.getPath()).addVersionLabel("1.0", "A-" + PUBLISHED, true);
        session.save();

        accessed.setProperty("test", "testA published under handle");
        session.save();

        this.property = publishedAccessed.getProperty("test");
        final Property property = BranchResolvePropertyResolver.getProperty(defaultPropertyResolver);
        assertThat(property.getString(), is("testAFromVersionHistory"));
    }
}