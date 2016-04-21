/*
 *  Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;
import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.autoexport.ResourceBundlesJSONSerializer.resourceBundlesToJSON;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;

public class ResourceBundleJSONSerializerTest {

    private Session session;

    @Before
    public void setUp() throws Exception {
        final MockNode translations = MockNode.root().addNode("hippo:configuration", NT_UNSTRUCTURED).addNode("hippo:translations", NT_RESOURCEBUNDLES);
        final MockNode foo = translations.addNode("foo", NT_RESOURCEBUNDLES);
        final MockNode bar = translations.addNode("bar", NT_RESOURCEBUNDLES);
        final Node quz = foo.addNode("quz", NT_RESOURCEBUNDLES);
        final Node quznl = quz.addNode("nl", NT_RESOURCEBUNDLE);
        final Node quzen = quz.addNode("en", NT_RESOURCEBUNDLE);
        quznl.setProperty("test", "test");
        quzen.setProperty("test", "test");
        final MockNode barnl = bar.addNode("nl", NT_RESOURCEBUNDLE);
        barnl.setProperty("test", "test");
        session = translations.getSession();
    }

    @Test
    public void testResourceBundlesToJSON() throws Exception {
        final DeltaInstruction rootInstruction = new DeltaInstruction(true, "hippo:translations", "combine", "/hippo:configuration/hippo:translations");
        final DeltaInstruction fooInstruction = new DeltaInstruction(true, "foo", "combine", "/hippo:configuration/hippo:translations/foo");
        rootInstruction.addInstruction(fooInstruction);
        final DeltaInstruction quzInstruction = new DeltaInstruction(true, "quz", "combine", "/hippo:configuration/hippo:translations/foo/quz");
        fooInstruction.addInstruction(quzInstruction);
        final DeltaInstruction quznlInstruction = new DeltaInstruction(true, "nl", "combine", "/hippo:configuration/hippo:translations/foo/quz/nl");
        quzInstruction.addInstruction(quznlInstruction);
        final DeltaInstruction propertyInstruction = new DeltaInstruction(false, "test", "", "/hippo:configuration/hippo:translations/foo/quz/nl");
        quznlInstruction.addInstruction(propertyInstruction);
        final DeltaInstruction barInstruction = new DeltaInstruction(true, "bar", "", "/hippo:configuration/hippo:translations/bar");
        rootInstruction.addInstruction(barInstruction);
        String expectation = "{\n" +
                "  \"foo\": {\"quz\": {\"nl\": {\"test\": \"test\"}}},\n" +
                "  \"bar\": {\"nl\": {\"test\": \"test\"}}\n" +
                "}";
        assertEquals(expectation, resourceBundlesToJSON(session, rootInstruction).toString(2));
    }

}
