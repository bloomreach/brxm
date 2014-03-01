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
package org.hippoecm.repository.reviewedactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.junit.Before;
import org.junit.Test;

public class VersioningWorkflowTest extends ReviewedActionsWorkflowAbstractTest {


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();

        root = root.addNode("test");
        node = root.addNode("versiondocument", "hippo:handle");
        node.addMixin("mix:referenceable");
        final ValueFactory valueFactory = session.getValueFactory();
        node.setProperty(HippoNodeType.HIPPO_DISCRIMINATOR, new Value[]{valueFactory.createValue("hippostd:state")});
        node = node.addNode("versiondocument", "hippo:testdocument");
        node.addMixin("mix:versionable");
        node.addMixin("hippostdpubwf:document");
        node.addMixin("hippostd:publishableSummary");
        node.setProperty("hippo:availability", new String[]{"preview"});
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostdpubwf:createdBy", "admin");
        node.setProperty("hippostdpubwf:creationDate", valueFactory.createValue("2010-02-04T16:32:28.068+02:00", PropertyType.DATE));
        node.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        node.setProperty("hippostdpubwf:lastModificationDate", valueFactory.createValue("2010-02-04T16:32:28.068+02:00", PropertyType.DATE));
        node.setProperty("counter", 0);

        node = root.addNode("baredocument", "hippo:testdocument");
        node.addMixin("mix:versionable");
        node.addMixin("hippostdpubwf:document");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostdpubwf:createdBy", "admin");
        node.setProperty("hippostdpubwf:creationDate", valueFactory.createValue("2010-02-04T16:32:28.068+02:00", PropertyType.DATE));
        node.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        node.setProperty("hippostdpubwf:lastModificationDate", valueFactory.createValue("2010-02-04T16:32:28.068+02:00", PropertyType.DATE));
        node.setProperty("counter", 0);

        session.save();
    }

    @Test
    public void testVersioning() throws WorkflowException, RepositoryException, RemoteException {
        Node node;
        Document document;

        class DocVersion {
            final Long counter;
            final String state;

            DocVersion(Node node) throws RepositoryException {
                this.counter = node.getProperty("counter").getLong();
                this.state = node.getProperty("hippostd:stateSummary").getString();
            }

            @Override
            public String toString() {
                return "DocVersion[count='" + counter + "', state='" + state + "']";
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == this) {
                    return true;
                }
                if (!(obj instanceof DocVersion)) {
                    return false;
                }
                DocVersion that = (DocVersion) obj;
                return that.counter.equals(counter) && that.state.equals(state);
            }
        }

        List<DocVersion> expected = new LinkedList<DocVersion>();

        edit();
        publish();
        expected.add(new DocVersion(getUnpublished()));

        edit();
        publish();
        expected.add(new DocVersion(getUnpublished()));

        edit();
        publish();
        expected.add(new DocVersion(getUnpublished()));

        edit();
        edit();
        publish();
        expected.add(new DocVersion(getUnpublished()));

        depublish();
        expected.add(new DocVersion(getUnpublished()));
        edit();
        edit();

        publish();
        expected.add(new DocVersion(getUnpublished()));

        edit();
        publish();
        expected.add(new DocVersion(getUnpublished()));

        node = getUnpublished();
        assertNotNull(node);

        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        Map<Calendar, Set<String>> history = versionwf.list();
        LinkedList<DocVersion> versions = new LinkedList<DocVersion>();
        for (Map.Entry<Calendar, Set<String>> entry : history.entrySet()) {
            document = versionwf.retrieve(entry.getKey());
            Node version = document.getNode(session);
            versions.add(new DocVersion(version));
        }

        /* FIXME: sometimes this test fails because of invalid earlier data in the repository */
        assertEquals(expected.size(), versions.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), versions.get(i));
        }

        restore(history.keySet().iterator().next());
        assertNotNull(getNode("test/versiondocument/versiondocument[@hippostd:state='published']"));
        assertNotNull(getUnpublished());

        restore(history.keySet().iterator().next());
    }

    private Node getUnpublished() throws RepositoryException {
        return getNode("test/versiondocument/versiondocument[@hippostd:state='unpublished']");
    }

    private void restore(Calendar historic) throws WorkflowException, RepositoryException, RemoteException {
        Node node = getUnpublished();
        assertNotNull(node);
        node.getParent().checkout();
        node.checkout();
        session.save();
        VersionWorkflow versionwf = (VersionWorkflow) getWorkflow(node, "versioning");
        versionwf.restore(historic);
        session.save();
        session.refresh(false);
    }

    private void edit() throws WorkflowException, RepositoryException, RemoteException {
        Node node;
        FullReviewedActionsWorkflow publishwf;
        Document document;

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
        node = document.getNode(session);
        Long counter = node.getProperty("counter").getLong();
        node.setProperty("counter", counter + 1);
        session.save();
        session.refresh(false);

        // commit edit
        node = document.getNode(session);
        publishwf = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        publishwf.commitEditableInstance();
        session.save();
        session.refresh(false);
    }

    private void publish() throws WorkflowException, RepositoryException, RemoteException {
        Node node = getUnpublished();
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
