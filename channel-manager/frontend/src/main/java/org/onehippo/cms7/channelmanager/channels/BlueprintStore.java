/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.channels;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.rest.BlueprintService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ChannelManagerHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtDataField;
import org.wicketstuff.js.ext.data.ExtJsonStore;

public class BlueprintStore extends ExtJsonStore<Object> {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_HAS_CONTENT_PROTOTYPE = "hasContentPrototype";
    private static final String FIELD_CONTENT_ROOT = "contentRoot";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BlueprintStore.class);

    private transient List<Blueprint> blueprints;
    private Long total;
    private final IRestProxyService restProxyService;

    public BlueprintStore(IRestProxyService restProxyService) {
        super(Arrays.asList(new ExtDataField(FIELD_NAME), new ExtDataField(FIELD_DESCRIPTION), new ExtDataField(FIELD_HAS_CONTENT_PROTOTYPE), new ExtDataField(FIELD_CONTENT_ROOT)));
        this.restProxyService = restProxyService;
    }

    @Override
    public void renderHead(Component component, final IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(ChannelManagerHeaderItem.get());
    }

    @Override
    protected long getTotal() {
        if (this.total == null) {
            List<Blueprint> blueprintList = getBlueprints();
            if (blueprintList != null){
                this.total = (long) blueprintList.size();
            } else {
                log.warn("Unable to retrieve blueprints.");
                this.total = 0l;
            }
        }
        return this.total;
    }

    public boolean isEmpty() {
        return getTotal() <= 0;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        final JSONObject properties = super.getProperties();
        Map<String, String> baseParams = new HashMap<String, String>();
        baseParams.put("xaction", "read");
        properties.put("baseParams", baseParams);
        return properties;

    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray data = new JSONArray();
        List<Blueprint> blueprints = getBlueprints();
        this.total = (long) blueprints.size();
        for (Blueprint blueprint : blueprints) {
            JSONObject object = new JSONObject();
            object.put("id", blueprint.getId());
            object.put(FIELD_NAME, blueprint.getName());
            object.put(FIELD_DESCRIPTION, blueprint.getDescription());

            boolean hasPrototype = blueprint.getHasContentPrototype();
            object.put(FIELD_HAS_CONTENT_PROTOTYPE, hasPrototype);

            Channel channel = blueprint.getPrototypeChannel();
            object.put(FIELD_CONTENT_ROOT, channel.getContentRoot());

            data.put(object);
        }
        return data;
    }

    private List<Blueprint> getBlueprints() {
    	if (blueprints == null) {
            if (restProxyService != null) {
                BlueprintService blueprintService = restProxyService.createRestProxy(BlueprintService.class);
                blueprints = blueprintService.getBlueprints();
            } else {
                blueprints = Collections.emptyList();
            }
        }

    	return blueprints;
    }

}
