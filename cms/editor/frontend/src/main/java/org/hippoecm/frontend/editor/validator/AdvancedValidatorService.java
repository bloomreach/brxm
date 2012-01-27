package org.hippoecm.frontend.editor.validator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.editor.validator.plugins.AbstractValidatorPlugin;
import org.hippoecm.frontend.editor.validator.plugins.IValidatorPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 */
public class AdvancedValidatorService extends Plugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(AdvancedValidatorService.class);

    public static final String VALIDATOR_SERVICE_ID = "validator.instance.service.id";

    private Map<String, IValidatorPlugin> map = new HashMap<String, IValidatorPlugin>();

    public AdvancedValidatorService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerTracker(new ServiceTracker<AbstractValidatorPlugin>(AbstractValidatorPlugin.class) {
            @Override
            protected void onServiceAdded(AbstractValidatorPlugin service, String name) {
                map.put(service.getName(), service);
            }

            @Override
            protected void onRemoveService(AbstractValidatorPlugin service, String name) {
                map.remove(service.getName());
            }
        }, VALIDATOR_SERVICE_ID);

        context.registerService(this, config.getString("advanced.validator.service.id", "advanced.validator.service"));
    }

    public IValidatorPlugin getValidator(String name) {
        if (StringUtils.isNotEmpty(name) && map.containsKey(name)) {
            return map.get(name);
        }
        return null;
    }

    public boolean containsValidator(String name) {
        return map.containsKey(name);
    }

    public boolean isEmpty(){
        return map.isEmpty();
    }


}
