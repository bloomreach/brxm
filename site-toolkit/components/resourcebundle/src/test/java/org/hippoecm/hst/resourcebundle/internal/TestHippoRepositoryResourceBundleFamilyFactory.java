/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle.internal;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;

import org.easymock.EasyMock;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * TestHippoRepositoryResourceBundleFamilyFactory
 */
public class TestHippoRepositoryResourceBundleFamilyFactory {

    @Test
    public void testPredefinedKeyValueReplacements() throws Exception {
        Repository repository = EasyMock.createNiceMock(Repository.class);
        Credentials liveCredentials = EasyMock.createNiceMock(Credentials.class);
        Credentials previewCredentials = EasyMock.createNiceMock(Credentials.class);

        EasyMock.replay(repository);
        EasyMock.replay(liveCredentials);
        EasyMock.replay(previewCredentials);

        HippoRepositoryResourceBundleFamilyFactory factory = new HippoRepositoryResourceBundleFamilyFactory(repository, liveCredentials, previewCredentials);

        String [] keys = { "key.first", "key.second", "key.third", "key.fourth", "key.fifth", "key.sixth" };
        String [] values = { "Hello", "${key.first}, World!", "Greeting - ${key.second}",
                "${key.first} was evaluated to ${key.second}", " ${key.first} was evaluated and no trimming ",
                "[<missing>]"};
        Map<String,String> messages = factory.createListResourceBundleContents(keys, values);

        assertEquals("Hello", messages.get("key.first"));
        assertEquals("Hello, World!", messages.get("key.second"));
        assertEquals("Greeting - Hello, World!", messages.get("key.third"));
        assertEquals("Hello was evaluated to Hello, World!", messages.get("key.fourth"));
        assertEquals(" Hello was evaluated and no trimming ", messages.get("key.fifth"));
        assertEquals("[<missing>] values should be filtered later", "[<missing>]", messages.get("key.sixth"));
    }

}
