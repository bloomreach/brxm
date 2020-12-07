/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.formdata;


import java.util.Calendar;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.hst.toolkit.addon.formdata.FormDataCleanupJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class FormDataCleanupJobIT extends RepositoryTestCase {

    // items that are 45 seconds old are still current, those 75 seconds old are outdated
    private static final long current = 45 * 1000;
    private static final long outdated = 75 * 1000;

    // ensures unique formdata name to prevent name clashes
    private int counter = 0;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
        RepositoryTestCase.setUpClass();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        removeFormData();
    }

    @After
    public void tearDown() throws Exception {
        removeFormData();
        super.tearDown();
    }

    private void removeFormData() throws RepositoryException {
        for (Node formdata : new NodeIterable(session.getNode("/formdata").getNodes())) {
            formdata.remove();
        }
        session.save();
    }

    private void createFormDataNode(String folderPath, long creationTimeMillis) throws Exception {
        Node folder = session.getRootNode();
        for (String folderName : folderPath.substring(1).split("/")) {
            if (folder.hasNode(folderName)) {
                folder = folder.getNode(folderName);
            } else {
                folder = folder.addNode(folderName, "hst:formdatacontainer");
            }
        }
        Node newFormData = folder.addNode(String.valueOf(counter++), "hst:formdata");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(creationTimeMillis);
        newFormData.setProperty("hst:creationtime", calendar);
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
        createFormDataNode("/formdata", now - current);
        createFormDataNode("/formdata", now - outdated);
        Assert.assertEquals(2l, getFormdataNodes().getTotalSize());

        final FormDataCleanupJob cleanupJob = new FormDataCleanupJob();
        final RepositoryJobExecutionContext executionContext = createExecutionContext("");

        cleanupJob.execute(executionContext);

        Assert.assertEquals(1l, getFormdataNodes().getTotalSize());
    }

    private RepositoryJobExecutionContext createExecutionContext(final String excludePaths) throws RepositoryException {
        final RepositoryJobExecutionContext executionContext = EasyMock.createMock(RepositoryJobExecutionContext.class);
        EasyMock.expect(executionContext.createSystemSession()).andReturn(session.impersonate(new SimpleCredentials("admin", new char[]{})));
        EasyMock.expect(executionContext.getAttribute("minutestolive")).andReturn("1");
        EasyMock.expect(executionContext.getAttribute("batchsize")).andReturn("100");
        EasyMock.expect(executionContext.getAttribute("excludepaths")).andReturn(excludePaths);
        EasyMock.replay(executionContext);
        return executionContext;
    }

    @Test
    public void testExcludeFormDataCleanup() throws Exception {
        long now = System.currentTimeMillis();
        createFormDataNode("/formdata", now - current);
        createFormDataNode("/formdata/permanent", now - outdated);
        createFormDataNode("/formdata/abcd", now - current);
        createFormDataNode("/formdata", now - outdated);
        createFormDataNode("/formdata/permanent", now - current);
        createFormDataNode("/formdata/abcd", now - outdated);
        Assert.assertEquals(6l, getFormdataNodes().getTotalSize());

        final FormDataCleanupJob cleanupJob = new FormDataCleanupJob();
        final RepositoryJobExecutionContext executionContext = createExecutionContext("/formdata/permanent/|/formdata/abcd|");

        cleanupJob.execute(executionContext);

        Assert.assertEquals(5l, getFormdataNodes().getTotalSize());
    }

    @Ignore
    @Test
    public void testFormDataBulkCleanup() throws Exception {
        Random random = new Random();
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            createFormDataNode("/formdata/" + alphabet.charAt(random.nextInt(alphabet.length())), time - (i % 2 == 0 ? current : outdated));
        }
        final FormDataCleanupJob cleanupJob = new FormDataCleanupJob();
        final RepositoryJobExecutionContext executionContext = createExecutionContext("");
        cleanupJob.execute(executionContext);
        Assert.assertEquals(500, getFormdataNodes().getTotalSize());
    }
}
