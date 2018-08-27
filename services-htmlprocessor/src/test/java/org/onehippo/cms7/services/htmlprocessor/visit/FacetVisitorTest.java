/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.visit;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.service.FacetService;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;

public class FacetVisitorTest {

    private MockNode document;
    private Model<Node> documentModel;
    private MockNode doc1;
    private MockNode doc2;

    @Before
    public void setUp() throws Exception {
        final MockNode root = MockNode.root();
        document = root.addNode("document", "hippo:document");
        documentModel = Model.of(document);

        doc1 = root.addNode("doc1", "nt:unstructured");
        doc2 = root.addNode("doc2", "nt:unstructured");
    }


    @Test
    public void callsTagProcessors() throws Exception {
        final Tag image = HtmlTag.from("img");
        final FacetTagProcessor processor = createMock(FacetTagProcessor.class);

        processor.onRead(eq(image), anyObject(FacetService.class));
        expectLastCall();
        processor.onWrite(eq(image), anyObject(FacetService.class));
        expectLastCall();
        replay(processor);

        final FacetVisitor visitor = new FacetVisitor(documentModel, processor);
        visitor.before();
        visitor.onRead(null, image);
        visitor.onWrite(null, image);
        visitor.after();
    }

    @Test(expected=NullPointerException.class)
    public void facetServiceIsNullifiedAfterRead() throws Exception {
        final Tag image = HtmlTag.from("img");
        final FacetTagProcessor processor = createMock(FacetTagProcessor.class);

        final FacetVisitor visitor = new FacetVisitor(documentModel, processor);
        visitor.before();
        visitor.after();

        visitor.onRead(null, image);
    }

    @Test(expected=NullPointerException.class)
    public void facetServiceIsNullifiedOnWrite() throws Exception {
        final Tag image = HtmlTag.from("img");
        final FacetTagProcessor processor = createMock(FacetTagProcessor.class);

        final FacetVisitor visitor = new FacetVisitor(documentModel, processor);
        visitor.before();
        visitor.after();

        visitor.onWrite(null, image);
    }
}
