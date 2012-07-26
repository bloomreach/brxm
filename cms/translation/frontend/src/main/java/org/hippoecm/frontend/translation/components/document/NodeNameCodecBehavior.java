/*
 *  Copyright 2010 Hippo.
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
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.repository.api.StringCodec;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.AbstractExtBehavior;

final class NodeNameCodecBehavior extends AbstractExtBehavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeNameCodecBehavior.class);

    private final IModel<StringCodec> codec;
    
    NodeNameCodecBehavior(IModel<StringCodec> codec) {
        this.codec = codec;
    }
    
    public void onRequest() {
        final RequestCycle requestCycle = RequestCycle.get();
        String name = requestCycle.getRequest().getParameter("name");
        final JSONObject response = new JSONObject();
        try {
            response.put("data", codec.getObject().encode(name));
            response.put("success", true);
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
        IRequestTarget requestTarget = new IRequestTarget() {

            public void respond(RequestCycle requestCycle) {
                WebResponse r = (WebResponse) requestCycle.getResponse();

                // Determine encoding
                final String encoding = Application.get().getRequestCycleSettings()
                        .getResponseRequestEncoding();
                r.setCharacterEncoding(encoding);
                r.setContentType("application/json;charset=" + encoding);

                // Make sure it is not cached
                r.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                r.setHeader("Cache-Control", "no-cache, must-revalidate");
                r.setHeader("Pragma", "no-cache");

                r.write(response.toString());
            }

            public void detach(RequestCycle requestCycle) {
            }

        };
        requestCycle.setRequestTarget(requestTarget);
    }

    @Override
    public void detach(Component component) {
        codec.detach();
        super.detach(component);
    }
}
