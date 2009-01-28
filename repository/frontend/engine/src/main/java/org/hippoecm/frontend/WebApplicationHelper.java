package org.hippoecm.frontend;

import org.apache.wicket.protocol.http.WebApplication;

public class WebApplicationHelper {

    private WebApplicationHelper() {
    }

    public static String getConfigurationParameter(WebApplication application, String parameterName, String defaultValue) {
        String result = application.getInitParameter(parameterName);
        if (result == null || result.equals("")) {
            result = application.getServletContext().getInitParameter(parameterName);
        }
        if (result == null || result.equals("")) {
            result = defaultValue;
        }
        return result;
    }
}
