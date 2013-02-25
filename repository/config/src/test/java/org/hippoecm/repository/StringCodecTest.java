/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class StringCodecTest extends RepositoryTestCase {

    private StringCodecFactory factory;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
        session.save();
        Map<String,StringCodec> codecs = new TreeMap<String,StringCodec>();
        codecs.put("encoding.node", new StringCodecFactory.UriEncoding());
        factory = new StringCodecFactory(codecs);
    }

    @Test
    public void testNodeNameCodec() throws RepositoryException {
        StringCodec codec = factory.getStringCodec("encoding.node");
        assertNotNull(codec);
        StringBuffer sb;
        for (int ch = 0; ch < 255; ch++) {
            sb = new StringBuffer();
            for (; (ch + 1) % 16 != 0; ch++)
                sb.append((char)ch);
            String name = new String(sb);
            String encoded = codec.encode(name);
            assertNotNull(encoded);
            if (encoded.equals(""))
                continue;
            try {
                session.getNode("/test").addNode(encoded);
                session.save();
            } catch (RepositoryException ex) {
                fail("failed to create node with name \"" + encoded + "\": " + ex.getMessage());
            }
        }
    }
}
