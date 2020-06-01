package ${groupId};

import java.util.Map;
import java.util.function.BiConsumer;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;

/**
 * ExampleInstruction
 */
public class ExampleInstruction implements Instruction {

    @Override
    public Status execute(Map<String, Object> parameters) {
        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Example instruction change message");
    }
}
