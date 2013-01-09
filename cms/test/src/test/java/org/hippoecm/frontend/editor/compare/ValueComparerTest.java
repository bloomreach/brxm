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
package org.hippoecm.frontend.editor.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.types.TypeException;
import org.junit.Test;

public class ValueComparerTest extends PluginTest {

    @Test
    public void testStreams() throws RepositoryException, TypeException, IOException {
        Node root = session.getRootNode();
        Node test = root.addNode("test", "nt:unstructured");
        Node a = test.addNode("a");
        Node b = test.addNode("b");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        osw.write("abx");
        osw.close();

        a.setProperty("z", new ByteArrayInputStream(os.toByteArray()));
        b.setProperty("z", new ByteArrayInputStream(os.toByteArray()));
        session.save();
        session.refresh(false);

        byte[] bytes = new byte[12];
        InputStream aStream = a.getProperty("z").getStream();
        int read = aStream.read(bytes);
        assertEquals(3, read);

        aStream = a.getProperty("z").getStream();
        InputStream bStream = b.getProperty("z").getStream();
        assertFalse(aStream.equals(bStream));

        StreamComparer comparer = new StreamComparer();
        assertTrue(comparer.areEqual(aStream, bStream));

        assertTrue(comparer.getHashCode(a.getProperty("z").getStream()) == comparer.getHashCode(b.getProperty("z")
                .getStream()));
    }
}
