package org.onehippo.cms7.essentials.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public final class RestUtils {

    private static final Logger log = LoggerFactory.getLogger(RestUtils.class);

    private RestUtils() {
    }

    public static RestfulList<PluginDescriptorRestful> parsePlugins(final String jsonString) {
        try {

            if (Strings.isNullOrEmpty(jsonString)) {
                return new RestfulList<>();
            }
            final ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            final RestfulList<PluginDescriptorRestful> restfulList = mapper.readValue(jsonString, RestfulList.class);
            return restfulList;
        } catch (Exception e) {
            log.error("Error parsing  plugins ", e);
        }
        return new RestfulList<>();
    }
}
