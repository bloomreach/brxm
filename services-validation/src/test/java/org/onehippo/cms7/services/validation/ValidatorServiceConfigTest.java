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
package org.onehippo.cms7.services.validation;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ValidatorServiceConfigTest {

    public static void assertLogMessage(final Log4jInterceptor interceptor, final String message, final Level level) {
        assertThat("There is a log message", interceptor.getEvents().size(), greaterThan(0));
        final LogEvent logEntry = interceptor.getEvents().get(0);
        assertThat(logEntry.getLevel(), is(level));
        assertThat(logEntry.getMessage().getFormattedMessage(), is(message));
    }

    @Test
    public void testReturnsNullIfNotFound() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");

        ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);
        assertNull(config.getValidator("validator-1"));

        final MockNode validator1 = configNode.addNode("validator-1", "nt:unstructured");
        validator1.setProperty("hipposys:className", "org.onehippo.cms7.services.validation.mock.MockValidator");
        config = new ValidatorServiceConfig(configNode);
        assertNull(config.getValidator("validator-2"));

        final MockNode validator2 = configNode.addNode("validator-2", "nt:unstructured");
        validator2.setProperty("hipposys:className", "org.onehippo.cms7.services.validation.mock.MockValidator");
        config.reconfigure(configNode);
        assertNull(config.getValidator("validator-3"));
    }

//    @Test(expected = ValidatorConfigurationException.class)
    public void testThrowsExceptionWhenValidatorCreationFailed() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");
        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);

        config.getValidator("validator-1");
    }

    @Test
    public void testReturnsNewHtmlProcessorFromConfig() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");
        final MockNode validator1 = configNode.addNode("validator-1", "nt:unstructured");
        validator1.setProperty("hipposys:className", "org.onehippo.cms7.services.validation.mock.MockValidator");

        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);
        assertNotNull(config.getValidator("validator-1"));

        final MockNode validator2 = configNode.addNode("validator-2", "nt:unstructured");
        validator2.setProperty("hipposys:className", "org.onehippo.cms7.services.validation.mock.MockValidator");

        config.reconfigure(configNode);
        assertNotNull(config.getValidator("validator-2"));
    }

    @Test
    public void testReturnsSameHtmlProcessorInstance() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");
        final MockNode validator1 = configNode.addNode("validator-1", "nt:unstructured");
        validator1.setProperty("hipposys:className", "org.onehippo.cms7.services.validation.mock.MockValidator");

        final ValidatorServiceConfig config = new ValidatorServiceConfig(configNode);

        assertEquals(config.getValidator("validator-1"), config.getValidator("validator-1"));
    }
}
