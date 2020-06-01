/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.channelmanager.channels;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.service.IRestProxyService;
import org.onehippo.cms7.services.hst.Channel;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtDataField;

/**
 * @version $Id$
 */
public class CountryGroupingChannelStore extends ChannelStore {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CountryGroupingChannelStore.class);

    public static final String UNKNOWN_COUNTRYCODE = "unknown_countrycode";

    public CountryGroupingChannelStore(final String storeId,
                                       final List<ExtDataField> fields,
                                       final String sortFieldName,
                                       final SortOrder sortOrder,
                                       final LocaleResolver localeResolver,
                                       final Map<String, IRestProxyService> restProxyServices,
                                       final BlueprintStore blueprintStore) {

        super(storeId, fields, sortFieldName, sortOrder, localeResolver, restProxyServices, blueprintStore);
    }

    protected void populateChannelRegion(final Channel channel, final JSONObject object) throws JSONException {
        String countryCode = getCountryCode(channel);
        Map<String, String> channelFieldValues = new HashMap<>();
        channelFieldValues.put("region", countryCode.toLowerCase());

        if (StringUtils.isNotBlank(countryCode)) {
            object.put("channelRegion", countryCode);
        }

        //Try to find the country flag icon in the repository using channel's country information (derived from its locale)
        String countryIconUrl = getChannelIconUrl(channelFieldValues, getChannelRegionIconPath());

        //else, try finding it in the repository but now using the channel's locale property (for backwards compatibility)
        if (StringUtils.isEmpty(countryIconUrl)) {
            //Fallback: we now consider the region field to have the same value as the locale
            String locale = channel.getLocale();
            if (StringUtils.isNotBlank(locale)) {
                channelFieldValues.put("region", locale.toLowerCase());
                countryIconUrl = getChannelIconUrl(channelFieldValues, getChannelRegionIconPath());
            }
        }

        //else, try finding it as a resource in the filesystem, using again that country property
        if (StringUtils.isEmpty(countryIconUrl)) {
            countryIconUrl = getIconResourceReferenceUrl(countryCode + ".png");
        }

        //else, try finding it as a resource in the filesystem, using again channel's locale property (for backwards compatibility)
        if (StringUtils.isEmpty(countryIconUrl)) {
            countryIconUrl = getIconResourceReferenceUrl(channel.getLocale() + ".png");
        }

        //else, show the default "unknown" country icon, this is loaded from filesystem and it always exists
        if (StringUtils.isEmpty(countryIconUrl)) {
            countryIconUrl = getIconResourceReferenceUrl(UNKNOWN_COUNTRYCODE + ".png");
        }

        object.put("channelRegionImg", countryIconUrl);
    }

    protected static String getCountryCode(final Channel channel){
        String channelLocaleAsString = channel.getLocale();
        try{
            Locale locale = LocaleUtils.toLocale(channelLocaleAsString);
            String countryCode = locale.getCountry();
            if (StringUtils.isNotBlank(countryCode)) {
                return countryCode;
            }
        } catch (IllegalArgumentException e){
            log.warn("Channel locale is not a legal locale. Channel name: {}, id: {}, locale: {}", new String[]{channel.getName(), channel.getId(), channel.getLocale()});
        }
        return UNKNOWN_COUNTRYCODE;
    }
}
