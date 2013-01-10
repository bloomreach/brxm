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

import java.rmi.RemoteException;
import java.util.Calendar;
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
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VersioningWorkflowTest extends RepositoryTestCase {

    static final String[] languages = new String[] { "aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av", "ay", "az", "ba", "be", "bg", "bh", "bi", "bm", "bn", "bo", "br", "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv", "cy", "da", "de", "dv", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr", "fy", "ga", "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr", "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it", "iu", "ja", "jv", "ka", "kg", "ki", "kj", "kk", "kl", "km", "kn", "kr", "ks", "ku", "kv", "kw", "ky", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml", "mn", "mo", "mr", "ms", "mt", "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv", "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm", "rn", "ro", "ru", "rw", "sa", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr", "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi", "yo", "za", "zh", "zu" };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();

        node = root.getNode("hippo:configuration/hippo:workflows");
        if (!node.hasNode("versioning")) {
            Node category = node.addNode("versioning", "hipposys:workflowcategory");
            node = category.addNode("version", "hipposys:workflow");
            node.setProperty("hipposys:nodetype", "hippo:document");
            node.setProperty("hipposys:display", "Versioning workflow");
            node.setProperty("hipposys:classname", "org.hippoecm.repository.standardworkflow.VersionWorkflowImpl");
            Node types = node.getNode("hipposys:types");
            node = types.addNode("org.hippoecm.repository.api.Document", "hipposys:type");
            node.setProperty("hipposys:nodetype", "hippo:document");
            node.setProperty("hipposys:display", "Document");
            node.setProperty("hipposys:classname", "org.hippoecm.repository.api.Document");

            node = category.addNode("restore", "hipposys:workflow");
            node.setProperty("hipposys:nodetype", "nt:frozenNode");
            node.setProperty("hipposys:display", "Versioning workflow");
            node.setProperty("hipposys:classname", "org.hippoecm.repository.standardworkflow.VersionWorkflowImpl");
            types = node.getNode("hipposys:types");
            node = types.addNode("org.hippoecm.repository.api.Document", "hipposys:type");
            node.setProperty("hipposys:nodetype", "hippo:document");
            node.setProperty("hipposys:display", "Document");
            node.setProperty("hipposys:classname", "org.hippoecm.repository.api.Document");
        }

        if (!root.hasNode("test")) {
            root = root.addNode("test");
        } else {
            root = root.getNode("test");
        }

        node = root.addNode("versiondocument", "hippo:handle");
        node.addMixin("hippo:hardhandle");
        node.addMixin("mix:referenceable");

        node = node.addNode("versiondocument", "hippo:document");
        node.addMixin("hippo:harddocument");
        node.addMixin("hippostd:publishable");
        node.addMixin("hippostd:languageable");
        node.setProperty("hippostd:state", "published");
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:language", "aa");

        node = root.addNode("baredocument", "hippo:document");
        node.addMixin("hippo:harddocument");
        node.addMixin("hippostd:publishable");
        node.addMixin("hippostd:languageable");
        node.setProperty("hippostd:state", "published");
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:language", "aa");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        super.tearDown();
    }

    @Test
    public void testSimple() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();

        if (!root.hasNode("test")) {
            node = root.addNode("test");
        } else {
            node = root.getNode("test");
        }
        node.addMixin("mix:versionable");
        session.save();
        node.checkin();
    }

    @Test
    public void testVersion() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        VersionWorkflow versionwf;
        Document document;

        Node node, root = session.getRootNode().getNode("test");

        node = root.getNode("baredocument");
        assertNotNull(node);
        versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        versionwf.version();
        session.refresh(false);

        node = root.getNode("baredocument");
        node.checkout();
        node.setProperty("hippostd:holder", node.getProperty("hippostd:holder").getString() + ".");
        session.save();

        versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        versionwf.version();
        session.refresh(false);

        node = root.getNode("baredocument");
        node.checkout();
        node.setProperty("hippostd:holder", node.getProperty("hippostd:holder").getString() + ".");
        session.save();

        versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        versionwf.version();
        session.refresh(false);

        node = root.getNode("baredocument");
        node.checkout();
        node.setProperty("hippostd:holder", node.getProperty("hippostd:holder").getString() + ".");
        session.save();
        versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        versionwf.version();
        session.refresh(false);

        node = root.getNode("baredocument");
        node.checkout();
        node.setProperty("hippostd:holder", node.getProperty("hippostd:holder").getString() + ".");
        session.save();
        session.refresh(false);
        versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        int count = 0;
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            document = versionwf.retrieve(entry.getKey());
            if(document != null) {
                Node version = session.getNodeByUUID(document.getIdentity());
                if (count > 0) {
                    assertEquals("admin" + "...........".substring(1, count++), version.getProperty("hippostd:holder").getString());
                } else {
                    ++count;
                }
            } else
                ++count;
        }
        assertEquals(5, count);
    }

    @Test
    public void testVersioning() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();
        Document document;
        Vector<String> expected = new Vector<String>();

        node = getNode("test/versiondocument");
        node.setProperty(HippoNodeType.HIPPO_DISCRIMINATOR, new Value[] { session.getValueFactory().createValue("hippostd:state") });

        edit();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());
        edit();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());
        version();
        edit();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        change("unpublished");
        version();
        expected.add("--");
        edit();
        version();
        change("published");
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());
        edit();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        String path = getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getPath();
        getNode("test/versiondocument").checkout();
        session.getWorkspace().copy(path, path + "x");
        Node copy = session.getRootNode().getNode(path.substring(1) + "x");
        getNode("test/versiondocument/versiondocument[@hippostd:state='published']").remove();
        copy.setProperty("hippostd:state", "unpublished");
        session.save();
        session.getWorkspace().move(path + "x", path);
        session.save();
        version();
        expected.add("--");

        edit();
        version();
        edit();
        version();
        getNode("test/versiondocument/versiondocument").checkout();
        getNode("test/versiondocument/versiondocument[@hippostd:state='unpublished']").setProperty("hippostd:state", "published");
        session.save();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        edit();
        edit();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        node = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        assertNotNull(node);
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        Vector<String> versions = new Vector<String>();
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            document = versionwf.retrieve(entry.getKey());
            if (document != null) {
                Node version = session.getNodeByUUID(document.getIdentity());
                versions.add(version.getProperty("hippostd:language").getString());
            } else {
                versions.add("--");
            }
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
    public void testResolution() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();
        Document document;
        Vector<String> expected = new Vector<String>();

        node = getNode("test/versiondocument");
        node.setProperty(HippoNodeType.HIPPO_DISCRIMINATOR, new Value[] { session.getValueFactory().createValue("hippostd:state") });

        edit();
        version();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        node = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        assertNotNull(node);
        
        VersionHistory jcrHistory = node.getVersionHistory();
        VersionIterator jcrIterator = jcrHistory.getAllVersions();
        jcrIterator.nextVersion();  // skip root version
        Version jcrVersion = jcrIterator.nextVersion();
        assertEquals(jcrVersion.getNode("jcr:frozenNode").getProperty("hippostd:state").getString(), "published");

        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(jcrVersion.getNode("jcr:frozenNode"), "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        Vector<String> versions = new Vector<String>();
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            document = versionwf.retrieve(entry.getKey());
            if (document != null) {
                Node version = session.getNodeByUUID(document.getIdentity());
                versions.add(version.getProperty("hippostd:language").getString());
            } else {
                versions.add("--");
            }
        }

        assertEquals(expected.size(), versions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), versions.get(i));
        }
    }

    private void edit() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node;
        getNode("test/versiondocument").checkout();
        node = getNode("test/versiondocument/versiondocument");
        node.checkout();
        assertNotNull(node);
        String language = node.getProperty("hippostd:language").getString();
        for (int i = 0; i < languages.length - 1; i++) {
            if (languages[i].equals(language)) {
                language = languages[i + 1];
                break;
            }
        }
        node.setProperty("hippostd:language", language);
        session.save();
        session.refresh(false);
    }

    private void change(String state) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node;
        getNode("test/versiondocument").checkout();
        node = getNode("test/versiondocument/versiondocument");
        node.checkout();
        assertNotNull(node);
        String language = node.getProperty("hippostd:language").getString();
        for (int i = 0; i < languages.length - 1; i++) {
            if (languages[i].equals(language)) {
                language = languages[i + 1];
                break;
            }
        }
        node.setProperty("hippostd:language", language);
        node.setProperty("hippostd:state", state);
        session.save();
        session.refresh(false);
    }

    private void version() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node = getNode("test/versiondocument/versiondocument");
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        assertNotNull(versionwf);
        versionwf.version();
        session.save();
        session.refresh(false);
    }
    protected WorkflowManager workflowMgr;

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
