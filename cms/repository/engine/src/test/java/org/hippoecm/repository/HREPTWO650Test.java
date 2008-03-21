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
 * See the License for the specific lang governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;

public class HREPTWO650Test extends FacetedNavigationAbstractTest {
    
    public void testDates() throws RepositoryException {
        numDocs = 0;
        commonStart();
        addNodeWithDate(session.getRootNode().getNode("documents"));
        addFacetDateSearch(session.getRootNode());
        session.save();
        assertTrue(session.getRootNode().getNode("facetdatesearch").getNodes().getSize() > 1 );
    }
    
    private void addFacetDateSearch(Node rootNode) throws RepositoryException {
        Node facetdatesearch = rootNode.addNode("facetdatesearch", HippoNodeType.NT_FACETSEARCH);
        facetdatesearch.setProperty(HippoNodeType.HIPPO_QUERYNAME, "fds");
        facetdatesearch.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents");
        facetdatesearch.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "date" });
    }

    private void addNodeWithDate(Node node) throws RepositoryException {
        Node datenode = node.addNode("datenode", "hippo:realdocument");
        Calendar cal = new GregorianCalendar();
        datenode.setProperty("date", cal);
        
        datenode = node.addNode("datenode2", "hippo:realdocument");
        cal = new GregorianCalendar();
        datenode.setProperty("date", cal);
    }
    
    public void testPerformance() throws RepositoryException, IOException {
    }
    
}
