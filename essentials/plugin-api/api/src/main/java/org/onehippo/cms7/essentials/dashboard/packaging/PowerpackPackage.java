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

package org.onehippo.cms7.essentials.dashboard.packaging;

import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;

/**
 * @version "$Id$"
 */
public interface PowerpackPackage {

    /**
     * Defines a group name. Each instruction set can have a several group names
     * <p></p>
     * (FYI by default, instruction set name  is "default").
     *
     * @return
     */
    Set<String> groupNames();

    /**
     * Returns parsed instructions
     *
     * @return instructions collection or null if not found
     */
    Instructions getInstructions();

    /**
     * Executes instructions
     *
     * @return executions status
     */
    InstructionStatus execute(final PluginContext context);
}
