/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.useractivity.UserActivityHeaderItem;
import org.hippoecm.frontend.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that pings the server regularly preventing unwanted session-timeouts.
 */
public class Pinger extends Label {

    private static final int DEFAULT_INTERVAL_SECONDS = 20;

    private static final Logger log = LoggerFactory.getLogger(Pinger.class);
    private static final String DEFAULT_WICKET_ID = "pinger";

    /**
     * Starts a default ping wicket components which uses a default frequency between ping intervals.
     * After the elapse of each interval a roundtrip to the server is made using an Ajax call.
     *
     * @param id the wicket id to use
     * @deprecated use {@link #every(Duration)} with a duration of 20 seconds.
     */
    @Deprecated
    public Pinger(String id) {
        this(id, Duration.seconds(DEFAULT_INTERVAL_SECONDS));
    }

    /**
     * Starts a default ping wicket components which uses the indicated duration between ping intervals.
     * After the elapse of each interval a roundtrip to the server is made using an Ajax call.
     * When the duration is negative, this wicket component behaves like a plain Label widget.
     *
     * @param id       the wicket id to use
     * @param interval the time to wait between ping intervals
     * @deprecated @deprecated use {@link #every(Duration)
     */
    @Deprecated
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

    @SuppressWarnings("deprecation")
    public static Pinger every(final Duration interval) {
        return new Pinger(DEFAULT_WICKET_ID, interval);
    }

    @SuppressWarnings("deprecation")
    public static Pinger dummy() {
        return new Pinger(DEFAULT_WICKET_ID, Duration.valueOf(-1));
    }

    @Override
    protected boolean getStatelessHint() {
        // Contrary to a normal label, the pinger is not stateless. Handles the situation where an open CMS tab for a
        // restarted CMS instance sends a PingBehavior call. Wicket will then lookup the pinger component on the default
        // first page (i.e. the login page), which is supposed to also contain a Pinger component (hence the fixed
        // Wicket ID "pinger"). Since a Pinger is not stateless, Wicket will then redirect to the current (login) page.
        return false;
    }

    private static class PingBehavior extends AbstractAjaxTimerBehavior {

        public PingBehavior(Duration duration) {
            super(duration);
            log.info("Pinger interval: {}", duration);
        }

        @Override
        protected void onTimer(AjaxRequestTarget target) {
            if (RequestUtils.isUserLoggedIn(RequestCycle.get())) {
                target.add(getComponent());
            }
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
