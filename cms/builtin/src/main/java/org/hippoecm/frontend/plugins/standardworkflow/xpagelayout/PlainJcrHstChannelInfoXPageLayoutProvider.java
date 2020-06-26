/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow.xpagelayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.services.hst.IXPageLayout;
import org.onehippo.cms7.services.hst.XPageLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Gets the {@link IXPageLayout}'s from the channel info node of the Channel identified by {@link #channelId}.
 * </p>
 * <p>The property key is {@link #X_PAGE_LAYOUTS} and the value is a Json array containing the layouts,
 * e.g. [{"label":"Layout 1","subPrototypeUUID":"uuid1","key":"layout1"},
 * {"label":"Layout 2","subPrototypeUUID":"uuid2","key":"layout2"}]</p>
 * <p>This class does not check the validity of the subPrototypeUUID's</p>
 */
public class PlainJcrHstChannelInfoXPageLayoutProvider implements XPageLayoutProvider {

    private static final Logger log = LoggerFactory.getLogger(PlainJcrHstChannelInfoXPageLayoutProvider.class);
    private static final String X_PAGE_LAYOUTS = "XPageLayouts";

    private final String channelId;

    public PlainJcrHstChannelInfoXPageLayoutProvider(final String channelId) {
        Objects.requireNonNull(channelId);
        this.channelId = channelId;
    }

    @Override
    public List<IXPageLayout> getXPageLayouts() {
        final HippoSession jcrSession = UserSession.get().getJcrSession();
        try {
            if (!XPageLayoutConstants.UNDEFINED_CHANNEL_ID.equals(channelId)) {
                final Node channelNode = jcrSession.getNode(channelId);
                if (channelNode.isNodeType(HstNodeTypes.NODENAME_HST_CHANNEL)) {
                    final Node channelInfoNode = channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
                    if (channelInfoNode.isNodeType(HstNodeTypes.NODENAME_HST_CHANNELINFO) &&
                            (channelInfoNode.hasProperty(X_PAGE_LAYOUTS))) {
                        final String xPageLayoutsString = channelInfoNode.getProperty(X_PAGE_LAYOUTS).getString();
                        return parseXPageLayoutsJSONString(xPageLayoutsString);
                    }
                } else {
                    log.debug("Node : { identifier : {} } is not a channel, please provide the uuid of a channel", channelId);
                }
            }
        } catch (RepositoryException e) {
            log.debug("Something went wrong when reading the xpagelayouts", e);
        }
        return Collections.emptyList();
    }

    List<IXPageLayout> parseXPageLayoutsJSONString(final String xPageLayoutsString) {
        List<IXPageLayout> result = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(xPageLayoutsString);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);
                result.add(new XPageLayout(
                        jsonObject.getString("key"),
                        jsonObject.getString("label"),
                        jsonObject.getString("subPrototypeUUID")));
            }
        } catch (JSONException e) {
            log.warn("{} is not a valid JSON string, please provide valid json", xPageLayoutsString);
        }
        return result;
    }


}
