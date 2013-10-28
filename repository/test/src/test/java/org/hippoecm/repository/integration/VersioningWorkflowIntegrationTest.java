/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.integration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VersioningWorkflowIntegrationTest extends RepositoryTestCase {

    private static final String[] languages = new String[] { "aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av", "ay", "az", "ba", "be", "bg", "bh", "bi", "bm", "bn", "bo", "br", "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv", "cy", "da", "de", "dv", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr", "fy", "ga", "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr", "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it", "iu", "ja", "jv", "ka", "kg", "ki", "kj", "kk", "kl", "km", "kn", "kr", "ks", "ku", "kv", "kw", "ky", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml", "mn", "mo", "mr", "ms", "mt", "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv", "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm", "rn", "ro", "ru", "rw", "sa", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr", "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi", "yo", "za", "zh", "zu" };

    private Node test;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        test = session.getRootNode().addNode("test");

        final Node handle = test.addNode("versiondocument", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        handle.addMixin("mix:referenceable");
        handle.setProperty(HippoNodeType.HIPPO_DISCRIMINATOR, new Value[] { session.getValueFactory().createValue("hippostd:state") });

        Node document = handle.addNode("versiondocument", "hippo:document");
        document.addMixin("mix:versionable");
        document.addMixin("hippostd:publishable");
        document.addMixin("hippostd:languageable");
        document.setProperty("hippostd:state", "published");
        document.setProperty("hippostd:holder", "admin");
        document.setProperty("hippostd:language", "aa");

        document = test.addNode("baredocument", "hippo:document");
        document.addMixin("mix:versionable");
        document.addMixin("hippostd:publishable");
        document.addMixin("hippostd:languageable");
        document.setProperty("hippostd:state", "published");
        document.setProperty("hippostd:holder", "admin");
        document.setProperty("hippostd:language", "aa");

        session.save();
    }

    @Test
    public void testVersionBareDocument() throws Exception {
        VersionWorkflow versionwf;
        Document document;

        Node node = test.getNode("baredocument");
        assertNotNull(node);

        for (int i = 0; i < 5; i++) {
            JcrUtils.ensureIsCheckedOut(node, false);
            node.setProperty("hippostd:holder", node.getProperty("hippostd:holder").getString() + ".");
            session.save();
            versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
            versionwf.version();
        }

        versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        int count = 0;
        final Set<Map.Entry<Calendar, Set<String>>> entries = history.entrySet();
        assertEquals("Unexpected number of versions", 5, entries.size());
        for (Map.Entry<Calendar, Set<String>> entry : entries) {
            document = versionwf.retrieve(entry.getKey());
            if(document != null) {
                Node version = session.getNodeByIdentifier(document.getIdentity());
                assertEquals("Version " + version.getParent().getName() + " contains unexpected holder property",
                        "admin" + "...........".substring(0, count+1), version.getProperty("hippostd:holder").getString());
            }
            count++;
        }
    }

    @Test
    public void testVersionHandledDocument() throws Exception {

        final List<String> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add(edit());
            version();
        }

        final Node published = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        assertNotNull(published);
        final VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(published, "versioning");
        final Map<Calendar, Set<String>> history = versionwf.list();
        final List<String> versions = new ArrayList<>();
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            final Document version = versionwf.retrieve(entry.getKey());
            assertNotNull(version);
            versions.add(version.getNode().getProperty("hippostd:language").getString());
        }

        assertEquals(expected.size(), versions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), versions.get(i));
        }
    }

    @Test
    /**
     * verify that, when using a frozen node to initialize the version workflow, it will
     * resolve the corresponding physical node.
     */
    public void testResolution() throws Exception {
        final Node document = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");

        final List<String> expected = new ArrayList<>();

        edit();
        version();
        expected.add(document.getProperty("hippostd:language").getString());
        
        VersionHistory jcrHistory = document.getVersionHistory();
        VersionIterator jcrIterator = jcrHistory.getAllVersions();
        jcrIterator.nextVersion();  // skip root version
        Version jcrVersion = jcrIterator.nextVersion();
        assertEquals(jcrVersion.getNode("jcr:frozenNode").getProperty("hippostd:state").getString(), "published");

        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(jcrVersion.getNode("jcr:frozenNode"), "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        Vector<String> versions = new Vector<String>();
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            Document version = versionwf.retrieve(entry.getKey());
            if (version != null) {
                versions.add(version.getNode().getProperty("hippostd:language").getString());
            } else {
                versions.add("--");
            }
        }

        assertEquals(expected.size(), versions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), versions.get(i));
        }
    }

    private String edit() throws Exception {
        final Node handle = getNode("test/versiondocument");
        JcrUtils.ensureIsCheckedOut(handle, false);
        final Node document = handle.getNode("versiondocument");
        JcrUtils.ensureIsCheckedOut(document, false);
        String language = document.getProperty("hippostd:language").getString();
        for (int i = 0; i < languages.length - 1; i++) {
            if (languages[i].equals(language)) {
                language = languages[i + 1];
                break;
            }
        }
        document.setProperty("hippostd:language", language);
        session.save();
        return language;
    }

    private void version() throws Exception {
        Node node = getNode("test/versiondocument/versiondocument");
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        assertNotNull(versionwf);
        versionwf.version();
        session.save();
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowManager.getWorkflow(category, canonicalNode);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
