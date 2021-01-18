/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.clustering;


import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.clustering.ClusterTest;

import static org.junit.Assert.assertEquals;


public class ClusteredIndexingDocumentsTest extends ClusterTest {

    @Test
    public void indexJcrCreatedDocumentsCluster() throws Exception {
        Session sessionNode1 = createSession(repo1);
        Session sessionNode2 = createSession(repo2);

        sessionNode2.refresh(false);

        try {
            final Node testFolder = sessionNode1.getRootNode().addNode("testfolder", "hippostd:folder");

            // although the issue from CMS-14290 already occured for total = 1, test 500 created handles just to make
            // sure no other indexing issues happen when indexing many jcr nodes
            int total = 500;
            for (int i = 1; i <= total; i++) {
                Node handle = testFolder.addNode("doc" + i, "hippo:handle");

                Node unpblishedDocVariant = handle.addNode("doc" + i, "sample:newsArticle");
                unpblishedDocVariant.setProperty("hippo:availability", new String[]{"preview"});
                Node body = unpblishedDocVariant.addNode("sample:body", "sample:body");
                body.setProperty("sample:title", "preview title");
                body.setProperty("sample:locale", new String[]{"en"});
                body.addNode("sample:illustration", "sample:illustration").setProperty("sample:imageSetId", "Id");

                sessionNode1.save();

                // change handle
                handle.addMixin("hippo:named");
                handle.setProperty("hippo:name", "MYNAME");

                sessionNode1.save();

                // add draft variant and 'save' directly, and copy all children to unpublished, to mimic obtain/commit
                // editable instance

                // on purpose, trigger many saves to force many transactions (and thus cluster syncs)
                Node draft = JcrUtils.copy(unpblishedDocVariant, "doc" + i, handle);
                draft.setProperty("hippo:availability", new String[]{});
                draft.setProperty("sample:authorId", 5l);
                sessionNode1.save();

                draft.getNode("sample:body").setProperty("sample:summary", "summary");
                draft.getNode("sample:body").getNode("sample:illustration").setProperty("sample:imageSetId", "NEWIDVALUE");
                sessionNode1.save();
                draft.addNode("sample:link").setProperty("sample:href", "HREFVALUE");

                sessionNode1.save();

                // commit mimic
                NodeIterator unpublishedVariantChildren = unpblishedDocVariant.getNodes();
                while (unpublishedVariantChildren.hasNext()) {
                    unpublishedVariantChildren.nextNode().remove();
                }

                NodeIterator draftVariantChildren = draft.getNodes();
                while (draftVariantChildren.hasNext()) {
                    Node next = draftVariantChildren.nextNode();
                    JcrUtils.copy(next, next.getName(), unpblishedDocVariant);
                }
                // mimic publish
                Node publishedVariant = JcrUtils.copy(unpblishedDocVariant, "doc" + i, handle);
                publishedVariant.setProperty("hippo:availability", new String[]{"live"});

                sessionNode1.save();

            }

            // trigger cluster sync
            sessionNode2.refresh(false);

            final String search0 = "/jcr:root/testfolder//element(*,sample:newsArticle)";
            final String search1 = "/jcr:root/testfolder//element(*,sample:newsArticle)[jcr:contains(., 'MYNAME')]";
            final String search2 = "/jcr:root/testfolder//element(*,sample:newsArticle)[jcr:contains(., 'NEWIDVALUE')]";
            final String search3 = "/jcr:root/testfolder//element(*,sample:newsArticle)[jcr:contains(., 'HREFVALUE')]";

            // every search for both sessions should result in the very same number of hits: 3 * the number of handles
            // since all nodes should match

            for (String xpath : new String[]{search0, search1, search2, search3}) {

                for (Session session : new Session[]{sessionNode1, sessionNode2}) {

                    QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();

                    // really iterate all the results
                    NodeIterator nodes = result.getNodes();
                    int count = 0;
                    while (nodes.hasNext()) {
                        nodes.nextNode();
                        count++;
                    }
                    assertEquals(String.format("Unexpected number of search results for '%s'",
                            session == sessionNode1 ?  "Cluster Node 1" : "Cluster Node 2"), total * 3, count);
                }
            }

        } finally {
            sessionNode1.logout();
            sessionNode2.logout();
        }

    }

    private Session createSession(Object repo) throws RepositoryException {
        final Session session = loginSession(repo);
        return session;
    }



}
