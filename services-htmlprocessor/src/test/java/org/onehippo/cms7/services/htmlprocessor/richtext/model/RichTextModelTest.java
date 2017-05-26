/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.model;

import javax.jcr.Node;

import org.easymock.EasyMock;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.model.HtmlProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class RichTextModelTest {

    @Test
    public void testRelease() throws Exception {
        final Model<String> valueModel = EasyMock.createMock(Model.class);
        valueModel.release();
        expectLastCall();

        final Model<Node> nodeModel = EasyMock.createMock(Model.class);
        nodeModel.release();
        // The node model is released by the RichTextProcessorModel directly and indirectly by the two internal visitors
        expectLastCall().times(3);

        replay(valueModel, nodeModel);

        final HtmlProcessorModel processorModel = new RichTextProcessorModel(valueModel, nodeModel, null, null);
        processorModel.release();

        verify(valueModel, nodeModel);
    }
}
