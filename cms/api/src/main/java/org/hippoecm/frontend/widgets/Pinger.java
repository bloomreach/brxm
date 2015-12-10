/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.useractivity.UserActivityHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that pings the server regularly preventing unwanted session-timeouts.
 */
public class Pinger extends Label {

    private static final int DEFAULT_INTERVAL_SECONDS = 20;

    private static final Logger log = LoggerFactory.getLogger(Pinger.class);

    /**
     * Starts a default ping wicket components which uses a default frequency between ping intervals.
     * After the elapse of each interval a roundtrip to the server is made using an Ajax call.
     * @param id the wicket id to use
     */
    public Pinger(String id) {
        this(id, Duration.seconds(DEFAULT_INTERVAL_SECONDS));
    }

    /**
     * Starts a default ping wicket components which uses the indicated duration between ping intervals.
     * After the elapse of each interval a roundtrip to the server is made using an Ajax call.
     * When the duration is negative, this wicket component behaves like a plain Label widget.
     * @param id       the wicket id to use
     * @param interval the time to wait between ping interfals
     */
    public Pinger(String id, Duration interval) {
        super(id);
        if (interval != null) {
            if (interval.greaterThan(0L)) {
                add(new PingBehavior(interval));
            }
        } else {
            add(new PingBehavior(Duration.seconds(DEFAULT_INTERVAL_SECONDS)));
        }
    }

    private static class PingBehavior extends AbstractAjaxTimerBehavior {

        public PingBehavior(Duration duration) {
            super(duration);
            log.info("Pinger interval: {}", duration.toString());
        }

        @Override
        protected void onTimer(AjaxRequestTarget target) {
            target.add(getComponent());
        }

        @Override
        protected void postprocessConfiguration(final JSONObject attributesJson, final Component component) throws JSONException {
            super.postprocessConfiguration(attributesJson, component);
            attributesJson.put(UserActivityHeaderItem.AJAX_ATTR_SYSTEM_ACTIVITY, true);
        }

        @Override
        protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
            super.updateAjaxAttributes(attributes);
            attributes.setChannel(new AjaxChannel("pinger", AjaxChannel.Type.DROP));
        }

    }
}
