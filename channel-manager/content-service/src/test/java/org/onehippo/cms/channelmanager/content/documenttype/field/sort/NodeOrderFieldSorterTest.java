/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({NamespaceUtils.class, FieldTypeContext.class})
public class NodeOrderFieldSorterTest {
    private final NodeOrderFieldSorter sorter = new NodeOrderFieldSorter();

    @Before
    public void setup() {
        PowerMock.mockStatic(NamespaceUtils.class);
        PowerMock.mockStatic(FieldTypeContext.class);
    }

    @Test
    public void sortFields() {
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final Node rootNode = createMock(Node.class);
        final Node node1 = createMock(Node.class);
        final Node node2 = createMock(Node.class);
        final Node node3 = createMock(Node.class);
        final FieldTypeContext fieldContext1 = createMock(FieldTypeContext.class);
        final FieldTypeContext fieldContext3 = createMock(FieldTypeContext.class);

        expect(NamespaceUtils.getEditorFieldConfigNodes(rootNode)).andReturn(Arrays.asList(node1, node2, node3));
        expect(FieldTypeContext.create(node1, context)).andReturn(Optional.of(fieldContext1));
        expect(FieldTypeContext.create(node2, context)).andReturn(Optional.empty());
        expect(FieldTypeContext.create(node3, context)).andReturn(Optional.of(fieldContext3));

        expect(context.getContentTypeRoot()).andReturn(rootNode);

        PowerMock.replayAll();
        replay(context);

        List<FieldTypeContext> fields = sorter.sortFields(context);

        assertThat(fields.size(), equalTo(2));
        assertThat(fields.get(0), equalTo(fieldContext1));
        assertThat(fields.get(1), equalTo(fieldContext3));

        verify(context);
        PowerMock.verifyAll();
    }

    @Test
    public void sortFieldsNoFields() {
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final Node rootNode = createMock(Node.class);

        expect(NamespaceUtils.getEditorFieldConfigNodes(rootNode)).andReturn(Collections.emptyList());

        expect(context.getContentTypeRoot()).andReturn(rootNode);

        PowerMock.replayAll();
        replay(context);

        List<FieldTypeContext> fields = sorter.sortFields(context);

        assertThat(fields.size(), equalTo(0));

        verify(context);
        PowerMock.verifyAll();
    }
}
