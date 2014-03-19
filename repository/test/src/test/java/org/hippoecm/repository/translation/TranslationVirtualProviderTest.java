/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.translation;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @deprecated  TranslationVirtualProvider is deprecated since 2.24.01
 */
@Deprecated
public class TranslationVirtualProviderTest extends RepositoryTestCase {

    static final String DOCUMENT_T9N_ID = "700a09f1-eac5-482c-a09e-ec0a6a5d6abc";
    static final String FOLDER_T9N_ID = "dbe51269-1211-4695-9dd4-1d6ea578f134";
    
    String[] content = new String[] {
        "/test",                "nt:unstructured",
        "/test/docs",           "hippo:testdocument",
            "jcr:mixinTypes",   "mix:versionable",

        "/test/docs/orig",      "hippo:handle",
            "jcr:mixinTypes",   "hippo:hardhandle",
        "/test/docs/orig/orig", "hippo:document",
            "jcr:mixinTypes",   "mix:versionable",

        "/test/docs/txn",       "hippo:handle",
            "jcr:mixinTypes",   "hippo:hardhandle",
        "/test/docs/txn/txn",  "hippo:testdocument",
            "jcr:mixinTypes",   "mix:versionable",

    };

    private Node origDoc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        build(content, session);

        origDoc = session.getRootNode().getNode("test/docs/orig/orig");
        origDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        origDoc.setProperty(HippoTranslationNodeType.LOCALE, "en");
        origDoc.setProperty(HippoTranslationNodeType.ID, DOCUMENT_T9N_ID);
        origDoc.addNode(HippoTranslationNodeType.TRANSLATIONS, HippoTranslationNodeType.TRANSLATIONS);
        session.save();
        session.refresh(false);
    }

    @Test
    public void testBasics() throws Exception {
        Node txnDoc = session.getRootNode().getNode("test/docs/txn/txn");
        txnDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        txnDoc.setProperty(HippoTranslationNodeType.LOCALE, "nl");
        txnDoc.setProperty(HippoTranslationNodeType.ID, DOCUMENT_T9N_ID);

        session.save();
        session.refresh(false);

        NodeIterator txns = session.getRootNode().getNode(
                "test/docs/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
        Map<String, List<Node>> translations = getTranslations(txns);
        assertTrue(translations.containsKey("en"));
        assertTrue(translations.containsKey("nl"));
        assertEquals(1, translations.get("en").size());
        assertEquals(1, translations.get("nl").size());
        assertEquals("/test/docs/orig/orig", ((HippoNode) translations.get("en").get(0)).getCanonicalNode().getPath());
        assertEquals("/test/docs/txn/txn", ((HippoNode) translations.get("nl").get(0)).getCanonicalNode().getPath());
    }

    private Map<String, List<Node>> getTranslations(NodeIterator txns) throws RepositoryException {
        Map<String, List<Node>> translations = new TreeMap<String, List<Node>>();
        while (txns.hasNext()) {
            Node translation = txns.nextNode();
            String name = translation.getName();
            List<Node> nodes;
            if (translations.containsKey(name)) {
                nodes= translations.get(name);
            } else {
                nodes = new LinkedList<Node>();
                translations.put(name, nodes);
            }
            nodes.add(translation);
        }
        return translations;
    }

    @Test
    public void testMultiRef() throws Exception {
        Node txnDoc = session.getRootNode().getNode("test/docs/txn/txn");
        txnDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        txnDoc.setProperty(HippoTranslationNodeType.LOCALE, "nl");
        txnDoc.setProperty(HippoTranslationNodeType.ID, DOCUMENT_T9N_ID);

        JcrUtils.copy(txnDoc.getParent(), txnDoc.getParent().getName(), txnDoc.getParent().getParent());

        session.save();
        session.refresh(false);

        Set<String> expected = new TreeSet<String>();
        expected.add("/test/docs/txn/txn");
        expected.add("/test/docs/txn[2]/txn");

        NodeIterator txns = session.getRootNode().getNode(
                "test/docs/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
        Map<String, List<Node>> translations = getTranslations(txns);
        assertTrue(translations.containsKey("nl"));
        for (Node translation : translations.get("nl")) {
            expected.remove(((HippoNode) translation).getCanonicalNode().getPath());
        }
        assertEquals(0, expected.size());
    }

    @Ignore
    public void testInheritContainerLocale() throws Exception {
        Node docsFolder = session.getRootNode().getNode("test/docs");
        docsFolder.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        docsFolder.setProperty(HippoTranslationNodeType.LOCALE, "nl");
        docsFolder.setProperty(HippoTranslationNodeType.ID, FOLDER_T9N_ID);

        Node txnDoc = session.getRootNode().getNode("test/docs/txn/txn");
        txnDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        txnDoc.setProperty(HippoTranslationNodeType.ID, DOCUMENT_T9N_ID);

        session.save();
        session.refresh(false);

        NodeIterator txns = session.getRootNode().getNode(
                "test/docs/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
        Map<String, List<Node>> translations = getTranslations(txns);
        assertTrue(translations.containsKey("nl"));
        assertEquals("/test/docs/txn/txn", ((HippoNode)translations.get("nl").get(0)).getCanonicalNode().getPath());
    }

    @Test
    public void testInheritsSingleFacetFilter() throws Exception {
        Node txnDoc = session.getRootNode().getNode("test/docs/txn/txn");
        txnDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        txnDoc.setProperty(HippoTranslationNodeType.LOCALE, "nl");
        txnDoc.setProperty(HippoTranslationNodeType.ID, DOCUMENT_T9N_ID);

        Node filter = session.getRootNode().getNode("test").addNode("filter", "hippo:facetselect");
        filter.setProperty("hippo:docbase", session.getRootNode().getNode("test/docs").getUUID());
        filter.setProperty("hippo:facets", new String[] { "state" });
        filter.setProperty("hippo:values", new String[] { "published" });
        filter.setProperty("hippo:modes", new String[] { "single" });

        session.save();
        session.refresh(false);

        assertTrue(session.getRootNode().getNode("test/docs/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS)
                .getNodes().hasNext());
        {
            txnDoc.setProperty("state", "published");

            session.save();
            session.refresh(false);

            NodeIterator txns = session.getRootNode().getNode(
                    "test/filter/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
            Map<String, List<Node>> translations = getTranslations(txns);
            assertTrue(translations.containsKey("nl"));
        }

        {
            txnDoc.setProperty("state", "unpublished");

            session.save();
            session.refresh(false);

            assertFalse(session.getRootNode().getNode("test/filter/txn").getNodes().hasNext());

            NodeIterator txns = session.getRootNode().getNode(
                    "test/filter/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
            Map<String, List<Node>> translations = getTranslations(txns);
            assertFalse(translations.containsKey("nl"));
        }
    }

    @Test
    public void facetSelectToDocumentHasDescendants() throws Exception {
        Node docsFolder = session.getRootNode().getNode("test/docs");
        docsFolder.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        docsFolder.setProperty(HippoTranslationNodeType.LOCALE, "nl");
        docsFolder.setProperty(HippoTranslationNodeType.ID, FOLDER_T9N_ID);
        docsFolder.addNode(HippoTranslationNodeType.TRANSLATIONS, HippoTranslationNodeType.TRANSLATIONS);
        
        Node filter = session.getRootNode().getNode("test").addNode("filter", "hippo:facetselect");
        filter.setProperty("hippo:docbase", session.getRootNode().getNode("test/docs").getUUID());
        filter.setProperty("hippo:facets", new String[] { "state" });
        filter.setProperty("hippo:values", new String[] { "unpublished" });
        filter.setProperty("hippo:modes", new String[] { "prefer-single" });

        session.save();
        session.refresh(false);

        assertTrue(session.itemExists("/test/filter/" + HippoTranslationNodeType.TRANSLATIONS + "/nl"));
    }
    
    @Test
    public void testInheritsPreferFacetFilter() throws Exception {
        Node txnDoc = session.getRootNode().getNode("test/docs/txn/txn");
        txnDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        txnDoc.setProperty(HippoTranslationNodeType.LOCALE, "nl");
        txnDoc.setProperty(HippoTranslationNodeType.ID, DOCUMENT_T9N_ID);

        Node filter = session.getRootNode().getNode("test").addNode("filter", "hippo:facetselect");
        filter.setProperty("hippo:docbase", session.getRootNode().getNode("test/docs").getUUID());
        filter.setProperty("hippo:facets", new String[] { "state" });
        filter.setProperty("hippo:values", new String[] { "unpublished" });
        filter.setProperty("hippo:modes", new String[] { "prefer-single" });

        session.save();
        session.refresh(false);

        assertTrue(session.getRootNode().getNode("test/docs/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS)
                .getNodes().hasNext());
        {
            txnDoc.setProperty("state", "published");

            session.save();
            session.refresh(false);

            NodeIterator txns = session.getRootNode().getNode(
                    "test/filter/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
            Map<String, List<Node>> translations = getTranslations(txns);
            assertTrue(translations.containsKey("nl"));
        }

        {
            txnDoc.setProperty("state", "unpublished");

            session.save();
            session.refresh(false);

            assertTrue(session.getRootNode().getNode("test/filter/txn").getNodes().hasNext());

            NodeIterator txns = session.getRootNode().getNode(
                    "test/filter/orig/orig/" + HippoTranslationNodeType.TRANSLATIONS).getNodes();
            Map<String, List<Node>> translations = getTranslations(txns);
            assertTrue(translations.containsKey("nl"));
        }
    }

}
