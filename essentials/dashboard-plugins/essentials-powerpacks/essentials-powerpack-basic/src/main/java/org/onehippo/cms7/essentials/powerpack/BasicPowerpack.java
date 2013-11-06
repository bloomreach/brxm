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

package org.onehippo.cms7.essentials.powerpack;

import java.io.InputStream;

import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class BasicPowerpack implements PowerpackPackage {

    private static Logger log = LoggerFactory.getLogger(BasicPowerpack.class);

    @Override
    public Instructions parseInstructions() {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/META-INF/instructions.xml");
        final StringBuilder myBuilder = GlobalUtils.readStreamAsText(resourceAsStream);
        final String content = myBuilder.toString();
        return InstructionParser.parseInstructions(content);
    }

    @Override
    public InstructionStatus execute() {
        return null;
    }
}
