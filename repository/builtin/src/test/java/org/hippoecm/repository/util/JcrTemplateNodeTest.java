/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import javax.jcr.Value;

import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.StringValue;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.junit.Test;

public class JcrTemplateNodeTest {

    @Test
    public void test_sealing_jcr_template_node() {

        JcrTemplateNode root = new JcrTemplateNode();

        root.addMixinName("hippo:testhtmlmixin")
                .addSingleValuedProperty("hippo:testsinglevaluedstring", new StringValue("test"))
                .addSingleValuedProperty("hippo:testsinglevaluedboolean", new BooleanValue(true))
                .addMultiValuedProperty("hippo:testmultivaluedstring", new Value[]{new StringValue("test")})
                .addMultiValuedProperty("hippo:testmultivaluedboolean", new Value[]{new BooleanValue(true)});

        final JcrTemplateNode child =
                root.addChild("child", "hippo:testhtml")
                .addSingleValuedProperty("hippo:testcontent", new StringValue("test"))
                .addMixinName("hippo:testhtmlmixin")
                .addSingleValuedProperty("hippo:testsinglevaluedstring", new StringValue("test"))
                .addSingleValuedProperty("hippo:testsinglevaluedboolean", new BooleanValue(true));

        JcrTemplateNode.seal(root);

        for (JcrTemplateNode current : new JcrTemplateNode[]{root , child}) {
            try {
                current.addChild("foo", "foo");
            } catch (UnsupportedOperationException e) {
                // expected
            }
            try {
                current.getChildren().add(new JcrTemplateNode());
            } catch (UnsupportedOperationException e) {
                // expected
            }
            try {
                current.getMixinNames().add("test");
            } catch (UnsupportedOperationException e) {
                // expected
            }
            try {
                current.getMultiValuedProperties().put("test", new Value[0]);
            } catch (UnsupportedOperationException e) {
                // expected
            }
            try {
                current.getSingleValuedProperties().put("test", ValueFactoryImpl.getInstance().createValue("foo"));
            } catch (UnsupportedOperationException e) {
                // expected
            }
        }

    }
}
