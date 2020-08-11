/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.stringcodec;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.StringCodecFactory.IdentEncoding;
import org.hippoecm.repository.api.StringCodecFactory.UriEncoding;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StringCodecModuleConfigTest {

    @Test
    public void initialConfigOnlyHasDefaultCodec() {
        final StringCodecModuleConfig config = new StringCodecModuleConfig();
        final StringCodecFactory factory = config.getStringCodecFactory();

        final StringCodec defaultCodec = factory.getStringCodec();
        assertNotNull(defaultCodec);

        assertThat(factory.getStringCodec("encoding"), equalTo(defaultCodec));
        assertThat(factory.getStringCodec("encoding", "locale"), equalTo(defaultCodec));
    }

    @Test
    public void emptyConfigHasDefaultCodec() {
        final StringCodecModuleConfig config = new StringCodecModuleConfig();
        final MockNode emptyNode = MockNode.root();

        config.reconfigure(emptyNode);

        final StringCodecFactory factory = config.getStringCodecFactory();
        assertThat(factory.getStringCodec().getClass(), equalTo(IdentEncoding.class));
    }

    @Test
    public void bootstrappedCodecs() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("encoding.display", "org.hippoecm.repository.api.StringCodecFactory$IdentEncoding");
        node.setProperty("encoding.node", "org.hippoecm.repository.api.StringCodecFactory$UriEncoding");

        final StringCodecModuleConfig config = new StringCodecModuleConfig();
        config.reconfigure(node);

        final StringCodecFactory factory = config.getStringCodecFactory();
        assertThat(factory.getStringCodec("encoding.display").getClass(), equalTo(IdentEncoding.class));
        assertThat(factory.getStringCodec("encoding.node").getClass(), equalTo(UriEncoding.class));
        assertThat(factory.getStringCodec().getClass(), equalTo(IdentEncoding.class));
    }

    @Test
    public void ignoresUnknownClasses() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("encoding.display", "no.such.Class");
        node.setProperty("encoding.node", "org.hippoecm.repository.api.StringCodecFactory$UriEncoding");

        final StringCodecModuleConfig config = new StringCodecModuleConfig();
        config.reconfigure(node);

        final StringCodecFactory factory = config.getStringCodecFactory();
        final StringCodec defaultCodec = factory.getStringCodec();
        assertThat(factory.getStringCodec("encoding.display"), equalTo(defaultCodec));
        assertThat(factory.getStringCodec("encoding.node").getClass(), equalTo(UriEncoding.class));
    }

    @Test
    public void ignoresOtherProperties() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("namespaced:property", "org.hippoecm.repository.api.StringCodecFactory$UriEncoding");
        node.setProperty("multipleProperty", new String[]{"org.hippoecm.repository.api.StringCodecFactory$UriEncoding"});
        node.setProperty("noStringProperty", true);
        node.setProperty("encoding.node", "org.hippoecm.repository.api.StringCodecFactory$UriEncoding");

        final StringCodecModuleConfig config = new StringCodecModuleConfig();
        config.reconfigure(node);

        final StringCodecFactory factory = config.getStringCodecFactory();
        final StringCodec defaultCodec = factory.getStringCodec();
        assertThat(factory.getStringCodec("namespaced:property"), equalTo(defaultCodec));
        assertThat(factory.getStringCodec("multipleProperty"), equalTo(defaultCodec));
        assertThat(factory.getStringCodec("noStringProperty"), equalTo(defaultCodec));
        assertThat(factory.getStringCodec("encoding.node").getClass(), equalTo(UriEncoding.class));
    }

    @Test
    public void reconfigureWithRepositoryExceptionKeepsOriginalConfig() throws RepositoryException {
        // configure with one 'test' encoding
        final MockNode okNode = MockNode.root();
        okNode.setProperty("test", "org.hippoecm.repository.api.StringCodecFactory$UriEncoding");

        final StringCodecModuleConfig config = new StringCodecModuleConfig();
        config.reconfigure(okNode);
        assertThat(config.getStringCodecFactory().getStringCodec("test").getClass(), equalTo(UriEncoding.class));

        // reconfigure with a bad node
        final Node badNode = createMock(Node.class);
        expect(badNode.getProperties()).andThrow(new RepositoryException());
        expect(badNode.getPath()).andReturn("/");
        replay(badNode);

        config.reconfigure(badNode);

        verify(badNode);

        // reading bad node fails so the original 'test' encoding should still exist
        assertThat(config.getStringCodecFactory().getStringCodec("test").getClass(), equalTo(UriEncoding.class));
    }
}