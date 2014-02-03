package org.onehippo.cms7.essentials.powerpack;

import java.io.InputStream;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.springframework.stereotype.Component;

/**
 * User: obourgeois
 * Date: 11-11-13
 */
@Component
public class BasicPowerpackWithSamples extends BasicPowerpack {

    @Inject
    private InstructionParser instructionParser;
    private Instructions instructions;
    @Override
    public Instructions getInstructions() {
        if (instructions == null) {
            final InputStream resourceAsStream = getClass().getResourceAsStream("/META-INF/instructions-with-samples.xml");
            final String content = GlobalUtils.readStreamAsText(resourceAsStream);
            instructions = instructionParser.parseInstructions(content);
        }
        return instructions;
    }
}
