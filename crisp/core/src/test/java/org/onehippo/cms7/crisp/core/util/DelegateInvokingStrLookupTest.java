/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.util;

import java.util.Properties;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DelegateInvokingStrLookupTest {

    private MyConfiguration myConfiguration;
    private StrSubstitutor substitutor;

    @Before
    public void setUp() throws Exception {
        final Properties props = new Properties();
        props.setProperty("var1", "value1");
        props.setProperty("var2", "value2");
        props.setProperty("var3", "value3");

        myConfiguration = new MyConfiguration(props);

        final DelegateInvokingStrLookup lookup = new DelegateInvokingStrLookup();
        lookup.setTargetObject(myConfiguration);
        lookup.setTargetMethod("getString");

        substitutor = new StrSubstitutor(lookup);
    }

    @Test
    public void testBasicScenario() throws Exception {
        assertEquals("value1", substitutor.replace("${var1}"));
        assertEquals("This is a variable: value2, the second one.", substitutor.replace("This is a variable: ${var2}, the second one."));
        assertEquals("This is variables: value3, ${var4}.", substitutor.replace("This is variables: ${var3}, ${var4}."));
        assertEquals("This is variables: value3, unknownValue.", substitutor.replace("This is variables: ${var3}, ${var4:-unknownValue}."));
    }

    public static class MyConfiguration {

        private final Properties props;

        private MyConfiguration(final Properties props) {
            this.props = props;
        }

        public String getString(String key) {
            return props.getProperty(key);
        }

    }
}
