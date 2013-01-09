/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationPlugin extends Plugin implements IFeedbackMessageFilter {


    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ValidationPlugin.class);

    private ValidationFeedback component;
    private JcrValidationService validation;

    public ValidationPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        validation = new JcrValidationService(context, config);
        component = new ValidationFeedback("component", validation.getModel());
        validation.start(component);

        if (config.getString(RenderService.FEEDBACK) != null) {
            context.registerService(this, config.getString(RenderService.FEEDBACK));
        } else {
            log.info("No feedback id {} defined", RenderService.FEEDBACK);
        }

        // TODO: listen for validation updates
    }

    public boolean accept(FeedbackMessage message) {
        return message.getReporter() == component;
    }

}
