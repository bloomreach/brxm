/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.repository.reviewedactions;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VersioningWorkflowTest extends ReviewedActionsWorkflowAbstractTest {


    static final String[] languages = new String[] { "aa", "ab", "ae", "af", "ak", "am", "an", "ar", "as", "av", "ay", "az", "ba", "be", "bg", "bh", "bi", "bm", "bn", "bo", "br", "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv", "cy", "da", "de", "dv", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr", "fy", "ga", "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr", "ht", "hu", "hy", "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it", "iu", "ja", "jv", "ka", "kg", "ki", "kj", "kk", "kl", "km", "kn", "kr", "ks", "ku", "kv", "kw", "ky", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml", "mn", "mo", "mr", "ms", "mt", "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr", "nv", "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm", "rn", "ro", "ru", "rw", "sa", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr", "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa", "wo", "xh", "yi", "yo", "za", "zh", "zu" };

    private static boolean delete(File path) {
        if(path.exists()) {
            if(path.isDirectory()) {
                for (File file : path.listFiles()) {
                    delete(file);
                }
            }
            return path.delete();
        }
        return false;
    }

    private static void clear() {
        String[] files = new String[] { ".lock", "repository", "version", "workspaces" };
        for (String file : files) {
            delete(new File(file));
        }
    }

    @Override
    @Before
    public void setUp() throws Exception {
        clear();
        super.setUp();

        Node node, root = session.getRootNode();

        node = root.getNode("hippo:configuration/hippo:workflows");
        
        if (node.hasNode("versioning")) {
            node.getNode("versioning").remove();
        }
        node = node.addNode("versioning", "hipposys:workflowcategory");
        node = node.addNode("version", "hipposys:workflow");
        node.setProperty("hipposys:nodetype", "hippo:document");
        node.setProperty("hipposys:display", "Versioning workflow");
        node.setProperty("hipposys:classname", "org.hippoecm.repository.standardworkflow.VersionWorkflowImpl");
        Node types = node.getNode("hipposys:types");
        node = types.addNode("org.hippoecm.repository.api.Document", "hipposys:type");
        node.setProperty("hipposys:nodetype", "hippo:document");
        node.setProperty("hipposys:display", "Document");
        node.setProperty("hipposys:classname", "org.hippoecm.repository.api.Document");

        if (root.hasNode("test")) {
            root.getNode("test").remove();
        }
        root = root.addNode("test");
        node = root.addNode("versiondocument", "hippo:handle");
        node.addMixin("hippo:hardhandle");
        node.addMixin("mix:referenceable");
        node.setProperty(HippoNodeType.HIPPO_DISCRIMINATOR, new Value[] { session.getValueFactory().createValue("hippostd:state") });
        node = node.addNode("versiondocument", "hippo:document");
        node.addMixin("hippo:harddocument");
        node.addMixin("hippostdpubwf:document");
        node.addMixin("hippostd:languageable");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:language", "aa");
        node.setProperty("hippostdpubwf:createdBy", "admin");
        node.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
        node.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        node.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");

        node = root.addNode("baredocument", "hippo:document");
        node.addMixin("hippo:harddocument");
        node.addMixin("hippostdpubwf:document");
        node.addMixin("hippostd:languageable");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:language", "aa");
        node.setProperty("hippostdpubwf:createdBy", "admin");
        node.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
        node.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        node.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");

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
    public void testVersioning() throws WorkflowException, RepositoryException, RemoteException {
        Node node;
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

        depublish();
        expected.add("--");
        edit();
        edit();

        publish();
        expected.add(getNode("test/versiondocument/versiondocument[@hippostd:state='published']").getProperty("hippostd:language").getString());

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

        /* FIXME: sometimes this test fails because of invalid earlier data in the repository */
        assertEquals(expected.size(), versions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), versions.get(i));
        }

        restore(history.keySet().iterator().next());
        assertNotNull(getNode("test/versiondocument/versiondocument[@hippostd:state='published']"));
        assertNotNull(getNode("test/versiondocument/versiondocument[@hippostd:state='unpublished']"));

        restore(history.keySet().iterator().next());
    }
    
    private void restore(Calendar historic) throws WorkflowException, RepositoryException, RemoteException {
        Node node = getNode("test/versiondocument/versiondocument");
        assertNotNull(node);
        node.getParent().checkout();
        node.checkout();
        session.save();
        Map<String, String[]> replacements = new TreeMap<String, String[]>();
        replacements.put("./hippostd:state", new String[] { "unpublished" });
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        versionwf.restore(historic, replacements);
        session.save();
        session.refresh(false);
    }

    private void edit() throws WorkflowException, RepositoryException, RemoteException {
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

    private void publish() throws WorkflowException, RepositoryException, RemoteException {
        Node node = getNode("test/versiondocument/versiondocument[@hippostd:state='unpublished']");
        assertNotNull(node);
        node.getParent().checkout();
        node.checkout();
        session.save();
        FullReviewedActionsWorkflow publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull(node);
        publishwf.publish();
        session.save();
        session.refresh(false);
    }

    private void depublish() throws WorkflowException, RepositoryException, RemoteException {
        Node node = getNode("test/versiondocument/versiondocument[@hippostd:state='published']");
        node.getParent().checkout();
        node.checkout();
        session.save();
        FullReviewedActionsWorkflow publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        publishwf.depublish();
        session.save();
        session.refresh(false);
    }
}
