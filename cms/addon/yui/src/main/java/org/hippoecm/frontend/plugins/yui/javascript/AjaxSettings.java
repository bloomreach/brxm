package org.hippoecm.frontend.plugins.yui.javascript;

import java.util.Map;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class AjaxSettings extends Settings {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    protected static final StringSetting CALLBACK_URL = new StringSetting("callbackUrl", "");
    protected static final StringSetting CALLBACK_FUNCTION = new StringSetting("callbackFunction", "", false);
    protected static final StringMapSetting CALLBACK_PARAMETERS = new StringMapSetting("callbackParameters", null);

    public AjaxSettings() {
        super();
    }
    
    public AjaxSettings(IPluginConfig config) {
        super(config);
    }
    
    @Override
    protected void initValues() {
        add(CALLBACK_URL, CALLBACK_FUNCTION, CALLBACK_PARAMETERS);
    }
    
    public void setCallbackUrl(String url) {
        CALLBACK_URL.set(url, this);
    }

    public void setCallbackFunction(String function) {
        CALLBACK_FUNCTION.set(function, this);
    }

    public void setCallbackParameters(Map<String, String> map) {
        CALLBACK_PARAMETERS.set(map, this);
    }

}
