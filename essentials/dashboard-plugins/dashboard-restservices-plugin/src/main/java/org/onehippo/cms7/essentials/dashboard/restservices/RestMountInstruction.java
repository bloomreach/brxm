/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.restservices;

import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class RestMountInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(RestMountInstruction.class);

    @Override
    public String getMessage() {
        return "Added rest mount to: {{restMountPath}}";
    }

    @Override
    public void setMessage(final String message) {

    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(final String action) {

    }

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {

        final Map<String, Object> myData = new HashMap<>();


        context.addPlaceholderData(myData);


        log.info("Executing RestMountInstruction...");
        return InstructionStatus.SUCCESS;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {

    }
}
