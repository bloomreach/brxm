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

package org.onehippo.cms7.essentials.dashboard.instructions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * @version "$Id$"
 */
@XmlTransient
public interface Instruction {

    String getMessage();

    void setMessage(String message);

    String getAction();

    void setAction(String action);

    InstructionStatus process(PluginContext context, InstructionStatus previousStatus);

    void processPlaceholders(final Map<String, Object> data);
}
