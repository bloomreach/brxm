/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.components.document;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.repository.api.StringCodec;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NodeNameCodecBehavior extends AbstractAjaxBehavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeNameCodecBehavior.class);

    private final IModel<StringCodec> codec;
    
    NodeNameCodecBehavior(IModel<StringCodec> codec) {
        this.codec = codec;
    }

    @Override
    public void onRequest() {
        final RequestCycle requestCycle = RequestCycle.get();
        StringValue name = requestCycle.getRequest().getRequestParameters().getParameterValue("name");
        final JSONObject response = new JSONObject();
        try {
            if (name != null) {
                response.put("data", codec.getObject().encode(name.toString()));
                response.put("success", true);
            } else {
                response.put("success", false);
            }
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
        IRequestHandler requestHandler = new IRequestHandler() {

            public void respond(IRequestCycle requestCycle) {
                WebResponse webResponse = (WebResponse) requestCycle.getResponse();

                // Determine encoding
                final String encoding = Application.get().getRequestCycleSettings()
                        .getResponseRequestEncoding();
                webResponse.setContentType("application/json;charset=" + encoding);

                // Make sure it is not cached
                webResponse.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                webResponse.setHeader("Cache-Control", "no-cache, must-revalidate");
                webResponse.setHeader("Pragma", "no-cache");

                webResponse.write(response.toString());
            }

            public void detach(IRequestCycle requestCycle) {
            }

        };
        requestCycle.scheduleRequestHandlerAfterCurrent(requestHandler);
    }

    @Override
    public void detach(Component component) {
        codec.detach();
        super.detach(component);
    }
}
