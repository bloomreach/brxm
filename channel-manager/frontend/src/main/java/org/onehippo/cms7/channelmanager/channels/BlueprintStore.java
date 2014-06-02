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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

import static org.onehippo.cms7.channelmanager.restproxy.RestProxyServicesManager.getExecutorService;

public class BlueprintStore extends ExtJsonStore<Object> {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_HAS_CONTENT_PROTOTYPE = "hasContentPrototype";
    private static final String FIELD_CONTENT_ROOT = "contentRoot";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BlueprintStore.class);

    private transient Map<String,Blueprint>  blueprints;
    private transient int availableRestProxiesHashCode;
    final Map<String, IRestProxyService> restProxyServices;

    public BlueprintStore(final Map<String, IRestProxyService> restProxyServices) {
        super(Arrays.asList(new ExtDataField(FIELD_NAME), new ExtDataField(FIELD_DESCRIPTION), new ExtDataField(FIELD_HAS_CONTENT_PROTOTYPE), new ExtDataField(FIELD_CONTENT_ROOT)));
        this.restProxyServices = restProxyServices;
    }

    @Override
    public void renderHead(Component component, final IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(ChannelManagerHeaderItem.get());
    }

    @Override
    protected long getTotal() {
        return getBlueprints().size();
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
        for (Blueprint blueprint : getBlueprints().values()) {
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

    public Map<String,Blueprint> getBlueprints() {
        // check whether previously loaded blueprints are from same live proxies as current live proxies
        if (blueprints != null && availableRestProxiesHashCode == restProxyServices.keySet().hashCode()) {
            return blueprints;
        }

        availableRestProxiesHashCode = restProxyServices.keySet().hashCode();
        blueprints = new HashMap<>();

        List<Callable<List<Blueprint>>> restProxyJobs = new ArrayList<>();
        for (final IRestProxyService restProxyService : restProxyServices.values()) {
            final BlueprintService blueprintService = restProxyService.createSecureRestProxy(BlueprintService.class);
            restProxyJobs.add(new Callable<List<Blueprint>>() {
                @Override
                public List<Blueprint> call() throws Exception {
                    return blueprintService.getBlueprints();
                }
            });
        }

        try {
            final List<Future<List<Blueprint>>> futures = getExecutorService().invokeAll(restProxyJobs);
            for (Future<List<Blueprint>> future : futures) {
                for (Blueprint blueprint : future.get()) {
                    blueprints.put(blueprint.getId(), blueprint);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to load the channels for one or more rest proxy.", e);
        }

        return blueprints;
    }


}
