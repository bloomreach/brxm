/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;


import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

import org.hippoecm.repository.Utilities;

import org.junit.*;
import static org.junit.Assert.*;

public class VersioningWorkflowTest extends ReviewedActionsWorkflowAbstractTest {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final String[] languages = new String[]{"aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av", "ay", "az", "ba", "be", "bg", "bh", "bi", "bm", "bn", "bo", "br", "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv", "cy", "da", "de", "dv", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr", "fy", "ga", "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr", "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it", "iu", "ja", "jv", "ka", "kg", "ki", "kj", "kk", "kl", "km", "kn", "kr", "ks", "ku", "kv", "kw", "ky", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml", "mn", "mo", "mr", "ms", "mt", "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv", "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm", "rn", "ro", "ru", "rw", "sa", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr", "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi", "yo", "za", "zh", "zu"};

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();

        node = root.getNode("hippo:configuration/hippo:workflows");
        if (!node.hasNode("versioning")) {
            node = node.addNode("versioning", "hippo:workflowcategory");
            node = node.addNode("version", "hippo:workflow");
            node.setProperty("hippo:nodetype", "hippo:document");
            node.setProperty("hippo:display", "Versioning workflow");
            node.setProperty("hippo:renderer", "");
            node.setProperty("hippo:classname", "org.hippoecm.repository.standardworkflow.VersionWorkflowImpl");
            Node types = node.getNode("hippo:types");
            node = types.addNode("org.hippoecm.repository.api.Document", "hippo:type");
            node.setProperty("hippo:nodetype", "hippo:document");
            node.setProperty("hippo:display", "Document");
            node.setProperty("hippo:classname", "org.hippoecm.repository.api.Document");
        }

        if (!root.hasNode("test")) {
            root = root.addNode("test");
        } else {
            root = root.getNode("test");
        }

        node = root.addNode("versiondocument", "hippo:handle");
        node.addMixin("mix:versionable"); // FIXME: should become: node.addMixin("hippo:hardhandle");
        node.addMixin("mix:referenceable");
        node = node.addNode("versiondocument", "hippo:document");
        node.addMixin("hippo:harddocument");
        node.addMixin("hippostd:publishable");
        node.addMixin("hippostd:languageable");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippostd:username", "admin");
        node.setProperty("hippostd:language", "aa");

        node = root.addNode("baredocument", "hippo:document");
        node.addMixin("hippo:harddocument");
        node.addMixin("hippostd:publishable");
        node.addMixin("hippostd:languageable");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippostd:username", "admin");
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
    public void testVersioning() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node, root = session.getRootNode();
        Document document;
        Vector<String> expected = new Vector<String>();

        edit();
        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        edit();
        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        edit();
        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        edit();
        edit();
        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        /*depublish();
        edit();
        edit();
        version();
        edit();
        version();

        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());*/

        edit();
        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

        node = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        assertNotNull(node);
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        Vector<String> versions = new Vector<String>();
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            document = versionwf.retrieve(entry.getKey());
            if(document != null) {
                Node version = session.getNodeByUUID(document.getIdentity());
                versions.add(version.getProperty("hippostd:language").getString());
            } else {
                versions.add("--");
            }
        }

        for (int i = 0; i < versions.size(); i++) {
            System.err.println("G " + versions.get(i));
        }
        for (int i = 0; i < expected.size(); i++) {
            System.err.println("E " + expected.get(i));
        }
        assertEquals(expected.size(), versions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), versions.get(i));
        }
    }

    private void edit() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node;
        FullReviewedActionsWorkflow publishwf;
        Document document;
        Property prop;

        getNode("test/versiondocument").checkout();

        // start edit
        node = getNode("test/versiondocument/versiondocument");
        assertNotNull(node);
        publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull(publishwf);
        document = publishwf.obtainEditableInstance();
        session.save();
        session.refresh(false);

        // edit
        node = session.getNodeByUUID(document.getIdentity());
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

        // commit edit
        node = session.getNodeByUUID(document.getIdentity());
        publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        publishwf.commitEditableInstance();
        session.save();
        session.refresh(false);
    }

    private void publish() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        // publish
        Node node = getNode("test/versiondocument/versiondocument[@hippostd:state='unpublished']");
        assertNotNull(node);
        FullReviewedActionsWorkflow publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull(node);
        publishwf.publish();
        session.save();
        session.refresh(false);

        // version
        node = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        assertNotNull(versionwf);
        versionwf.version();
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

    private void depublish() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        // publish
        Node node = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        node.getParent().checkout();
        node.checkout();
        FullReviewedActionsWorkflow publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        publishwf.depublish();
        session.save();
        session.refresh(false);

        // version
        node = getNode("test/versiondocument/versiondocument[@hippostd:state='unpublished']");
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        assertNotNull(versionwf);
        versionwf.version();
        session.save();
        session.refresh(false);
    }
}
