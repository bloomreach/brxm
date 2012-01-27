package org.hippoecm.frontend.editor.validator.plugins;

import org.apache.wicket.Session;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.IStringResourceProvider;
import org.hippoecm.frontend.editor.validator.AdvancedValidatorService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ITranslateService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @version $Id$
 */
abstract public class AbstractValidatorPlugin extends Plugin implements IValidatorPlugin, IStringResourceProvider {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(AbstractValidatorPlugin.class);

    private final String name;

    public AbstractValidatorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        name = config.getName().substring(config.getName().lastIndexOf(".") + 1);
        context.registerService(this, AdvancedValidatorService.VALIDATOR_SERVICE_ID);
    }

    public String getName() {
        return name;
    }

    protected String getTranslation() {
        return translateKey(getName());
    }

    protected String translateKey(String key) {
        return getString(getCriteria(key));
    }

    protected Map<String, String> getCriteria(String key) {
        Map<String, String> keys = new TreeMap<String, String>();
        String realKey;
        if (key.indexOf(',') > 0) {
            realKey = key.substring(0, key.indexOf(','));
            IValueMap map = new ValueMap(key.substring(key.indexOf(',') + 1));
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String) {
                    keys.put(entry.getKey(), (String) entry.getValue());
                }
            }
        } else {
            realKey = key;
        }
        keys.put(HippoNodeType.HIPPO_KEY, realKey);

        Locale locale = Session.get().getLocale();
        keys.put(HippoNodeType.HIPPO_LANGUAGE, locale.getLanguage());

        String value = locale.getCountry();
        if (value != null) {
            keys.put("country", locale.getCountry());
        }

        value = locale.getVariant();
        if (value != null) {
            keys.put("variant", locale.getVariant());
        }
        return keys;
    }

    public String getString(Map<String, String> criteria) {
        String[] translators = getPluginConfig().getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            for (String translatorId : translators) {
                ITranslateService translator = getPluginContext().getService(translatorId,
                        ITranslateService.class);
                if (translator != null) {
                    String translation = translator.translate(criteria);
                    if (translation != null) {
                        return translation;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getResourceProviderKey() {
        final String[] translators = getPluginConfig().getStringArray(ITranslateService.TRANSLATOR_ID);
        if (translators != null) {
            return Arrays.toString(translators);
        }
        return null;
    }


}
