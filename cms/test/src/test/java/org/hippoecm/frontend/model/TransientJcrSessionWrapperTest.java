/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.jcr.Session;

import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;


public class TransientJcrSessionWrapperTest extends PluginTest {

    @Test
    public void assert_serialization_deserialization_TransientJCrSessionWrapper() throws Exception {

        Session testSession = server.login("admin", "admin".toCharArray());
        TransientJcrSessionWrapper wrapper = new TransientJcrSessionWrapper(testSession);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(wrapper);
        bos.flush();

        // assert jcrSession is not live any more
        assertFalse(testSession.isLive());
        InputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        TransientJcrSessionWrapper deserWapper = (TransientJcrSessionWrapper) in.readObject();
        assertNull(deserWapper.session);
    }
}
