package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class StyleableTemplateDeviceModel extends SimpleStylableDeviceModel {

    private static Logger log = LoggerFactory.getLogger(StyleableTemplateDeviceModel.class);

    protected final Map<String, Object> templatedProperties = new HashMap<String, Object>();

    public StyleableTemplateDeviceModel(final IPluginConfig config) {
        super(config);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            templatedProperties.put(entry.getKey(), revolveBinaryOrOrdinayTemplateString(entry.getKey(), entry.getValue()));
        }
        templatedProperties.put("request.url", getRequestURL());
    }


    public String revolveBinaryOrOrdinayTemplateString(String key, Object value) {
        return key.startsWith("binary.") ? getBinaryUrl(process(value)) : process(value);
    }

    @Override
    public String getStyle() {
        final String style = super.getStyle();
        if (style != null) {
            return process(style);
        }
        return style;
    }

    @Override
    public String getWrapStyle() {
        final String wrapStyle = super.getWrapStyle();
        if (wrapStyle != null) {
            return process(wrapStyle);
        }
        return wrapStyle;
    }

    protected String process(final Object style) {
        if (style instanceof String) {
            return process((String) style, convertEntrySetToMap(config.entrySet()));
        }
        return process(String.valueOf(style), convertEntrySetToMap(config.entrySet()));
    }

    protected String process(final String style, final Map<String, Object> values) {
        MapVariableInterpolator mapVariableInterpolator = new MapVariableInterpolator(style,
                values);
        return mapVariableInterpolator.toString();
    }


    protected String process(final String style) {
        return process(style, templatedProperties);
    }

    protected Map<String, Object> convertEntrySetToMap(Set<Map.Entry<String, Object>> set) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : set) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    protected String getBinaryUrl(final String absRepoPath) {
        if (StringUtils.isEmpty(absRepoPath)) {
            return null;
        }
        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle != null) {
            javax.jcr.Session session = ((UserSession) requestCycle.getSession()).getJcrSession();
            try {
                if (session.nodeExists(absRepoPath)) {
                    String url = encodeUrl("binaries" + absRepoPath);
                    return requestCycle.getResponse().encodeURL(url).toString();
                }
            } catch (RepositoryException repositoryException) {
                log.error("Error getting the channel icon resource url.", repositoryException);
            }
        }
        return null;
    }

    protected String getRequestURL() {
        try {
            return ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getRequestURL().toString();
        } catch (Exception e) {
            log.error("error while trying to retrieve the request URL needed for the images", e);
        }
        return null;
    }

    private String encodeUrl(String path) {
        String[] elements = StringUtils.split(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = WicketURLEncoder.PATH_INSTANCE.encode(elements[i], "UTF-8");
        }
        return StringUtils.join(elements, '/');
    }

    public void add(String key, Object value) {
        templatedProperties.put(key, value);
    }


}
