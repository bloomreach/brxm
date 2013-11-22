package org.onehippo.cms7.essentials.powerpack;

import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;

import java.io.InputStream;

/**
 * User: obourgeois
 * Date: 11-11-13
 */
public class BasicPowerpackWithSamples extends BasicPowerpack {

    @Override
    public Instructions getInstructions() {
        if (instructions == null) {
            final InputStream resourceAsStream = getClass().getResourceAsStream("/META-INF/instructions-with-samples.xml");
            final String content = GlobalUtils.readStreamAsText(resourceAsStream);
            instructions = InstructionParser.parseInstructions(content);
        }
        return instructions;
    }
}
