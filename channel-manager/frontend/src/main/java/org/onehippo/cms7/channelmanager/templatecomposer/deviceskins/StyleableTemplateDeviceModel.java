/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
            templatedProperties.put(entry.getKey(), process(entry.getValue()));
        }
        //templatedProperties.put("request.url", getRequestURL());
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

    /*
    private String getRequestURL() {
        try {
            return ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getRequestURL().toString();
        } catch (Exception e) {
            log.error("error while trying to retrieve the request URL needed for the images", e);
        }
        return null;
    }
    */
}
