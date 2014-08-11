/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.hst.toolkit.addon.formdata;


import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class FormDataCleanupJobTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        NodeIterator nodes = getFormdataNodes();
        while(nodes.hasNext()) {
            nodes.nextNode().remove();
        }
        Node rootNode = session.getRootNode();
        rootNode.addNode("formdata", "hst:formdatacontainer");
        session.save();

    }

    @After
    public void tearDown() throws Exception {
        removeNode("/formdata");
        super.tearDown();
    }

    private void createFormDataNode(String subPath, long creationTimeMillis) throws Exception {
        Node rootNode = session.getRootNode();
        Node formData = rootNode.getNode("formdata");
        if (subPath != null) {
            if (!formData.hasNode(subPath)) {
                formData = formData.addNode(subPath, "hst:formdatacontainer");
            } else {
                formData = formData.getNode(subPath);
            }
        }
        Node postedFormDataNode = formData.addNode("tick_"+System.currentTimeMillis(), "hst:formdata");
        Thread.sleep(2l);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(creationTimeMillis);
        postedFormDataNode.setProperty("hst:creationtime", calendar);
        session.save();
    }

    private HippoNodeIterator getFormdataNodes() throws Exception {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        // need to order by, otherwise total size returned is always -1
        NodeIterator nodes = queryManager.createQuery("SELECT * FROM hst:formdata ORDER BY hst:creationtime ASC", Query.SQL).execute().getNodes();
        return (HippoNodeIterator)nodes;
    }

    @Test
    public void testFormDataCleanup() throws Exception {
        long now = System.currentTimeMillis();
        createFormDataNode(null, now - 45 * 1000);
        createFormDataNode(null, now - 65 * 1000);
        assertEquals(2l, getFormdataNodes().getTotalSize());

        final FormDataCleanupJob cleanupJob = new FormDataCleanupJob();
        final RepositoryJobExecutionContext executionContext = createMock(RepositoryJobExecutionContext.class);
        expect(executionContext.createSystemSession()).andReturn(session.impersonate(new SimpleCredentials("admin", new char[] {})));
        expect(executionContext.getAttribute("minutestolive")).andReturn("1");
        expect(executionContext.getAttribute("batchsize")).andReturn("100");
        expect(executionContext.getAttribute("excludepaths")).andReturn("");
        replay(executionContext);

        cleanupJob.execute(executionContext);

        assertEquals(1l, getFormdataNodes().getTotalSize());
    }

    @Test
    public void testExcludeFormDataCleanup() throws Exception {
        long now = System.currentTimeMillis();
        createFormDataNode(null, now - 45 * 1000);
        createFormDataNode("permanent", now - 45 * 1000);
        createFormDataNode("abcd", now - 45 * 1000);
        createFormDataNode(null, now - 65 * 1000);
        createFormDataNode("permanent", now - 65 * 1000);
        createFormDataNode("abcd", now - 65 * 1000);
        assertEquals(6l, getFormdataNodes().getTotalSize());

        final FormDataCleanupJob cleanupJob = new FormDataCleanupJob();
        final RepositoryJobExecutionContext executionContext = createMock(RepositoryJobExecutionContext.class);
        expect(executionContext.createSystemSession()).andReturn(session.impersonate(new SimpleCredentials("admin", new char[] {})));
        expect(executionContext.getAttribute("minutestolive")).andReturn("1");
        expect(executionContext.getAttribute("batchsize")).andReturn("100");
        expect(executionContext.getAttribute("excludepaths")).andReturn("/formdata/permanent/|/formdata/abcd|");
        replay(executionContext);

        cleanupJob.execute(executionContext);

        assertEquals(5l, getFormdataNodes().getTotalSize());
    }

}
