/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.Session;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IValidateService;
import org.junit.Before;
import org.junit.Test;

public class JcrValidatorPluginTest extends PluginTest {
    final static String[] content = {
        "/test", "nt:unstructured",
        "/test/plugin", "frontend:plugin",
        "plugin.class", JcrValidatorPlugin.class.getName(),
        "wicket.model", "service.model",
        "validator.id", "service.validator",
        "feedback.id", "service.feedback",
    };
    IPluginConfig config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        build(session, content);

        JcrNodeModel nodeModel = new JcrNodeModel("/test/content");
        ModelReference modelRef = new ModelReference("service.model", nodeModel);
        modelRef.init(context);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }

    protected List<FeedbackMessage> getFeedback() {
        FeedbackMessages feedbackMessages = Session.get().getFeedbackMessages();
        return feedbackMessages.messages(context.getService("service.feedback",
                IFeedbackMessageFilter.class));
    }

    protected void validate() {
        context.getService("service.validator", IValidateService.class).validate();
    }

    @Test
    public void testValidator() throws Exception {
        start(config);

        // [test:validator]
        // - test:optional (string)
        // - test:mandatory (string) mandatory
        // - test:autocreated (string) mandatory autocreated
        // - test:protected (string) mandatory protected

        Node content = root.getNode("test").addNode("content", "test:validator");
        validate();

        List<FeedbackMessage> messages = getFeedback();
        assertEquals(1, messages.size());
    }
}
