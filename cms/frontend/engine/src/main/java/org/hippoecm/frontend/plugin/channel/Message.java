/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin.channel;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.IPluginModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Message implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Message.class);

    private MessageContext context;
    private String operation;
    private IPluginModel model;

    public Message(String operation, IPluginModel model) {
        this.operation = operation;
        this.model = model;
        this.context = new MessageContext();
    }

    public String getOperation() {
        return operation;
    }

    public IPluginModel getModel() {
        return model;
    }

    public void setContext(MessageContext context) {
        this.context = context;
    }

    public MessageContext getContext() {
        return context;
    }

}
