/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlTransient;

import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;

import com.google.common.eventbus.EventBus;


/**
 * @version "$Id$"
 */
@XmlTransient
public abstract class PluginInstruction implements Instruction {

    public static final String COPY = "copy";
    public static final String DELETE = "delete";

    @Inject
    private EventBus eventBus;
    protected PluginInstruction() {
     /*   final Injector injector = Guice.createInjector(EventBusModule.getInstance());
        injector.injectMembers(this);*/
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        final String message = TemplateUtils.replaceTemplateData(getMessage(), data);
        if (message != null) {
            setMessage(message);
        }
    }


    protected void sendEvents() {
        eventBus.post(new InstructionEvent(this));
        eventBus.post(new DisplayEvent(getMessage()));
    }

}
