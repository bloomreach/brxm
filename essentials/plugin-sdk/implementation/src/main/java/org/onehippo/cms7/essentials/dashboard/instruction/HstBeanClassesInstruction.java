/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.service.WebXmlService;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.springframework.stereotype.Component;

/**
 * Instruction
 */
@Component
@XmlRootElement(name = "hstBeanClasses", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class HstBeanClassesInstruction extends BuiltinInstruction {

    @Inject WebXmlService webXmlService;
    private String pattern;

    public HstBeanClassesInstruction() {
        super(Type.EXECUTE);
    }

    @XmlAttribute
    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    @Override
    public Status execute(final PluginContext context) {
        return webXmlService.addHstBeanClassPattern(pattern)
                ? Status.SUCCESS : Status.FAILED;
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(getDefaultGroup(),
                "Add mapping '" + pattern + "' for annotated HST beans to Site web.xml.");
    }
}
