/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.logout;

import javax.servlet.http.HttpSession;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.settings.GlobalSettings;

public class ActiveLogoutSettings {

    public static final String CONFIG_MAX_INACTIVE_INTERVAL = "max.inactive.interval";

    private static final MetaDataKey<Boolean> STAY_SIGNED_IN = new MetaDataKey<Boolean>() {};

    private ActiveLogoutSettings() {
    }

    public static ActiveLogoutSettings get() {
        return new ActiveLogoutSettings();
    }

    public boolean isEnabled() {
        return getMaxInactiveIntervalSeconds() > 0;
    }

    public int getMaxInactiveIntervalSeconds() {
        final IPluginConfig globalSettings = GlobalSettings.get();
        final Duration duration = globalSettings.getAsDuration(CONFIG_MAX_INACTIVE_INTERVAL);
        return duration != null ? (int) duration.seconds() : getDefaultMaxInactiveIntervalSeconds();
    }

    private static int getDefaultMaxInactiveIntervalSeconds() {
        final ServletWebRequest servletRequest = (ServletWebRequest) RequestCycle.get().getRequest();
        final HttpSession httpSession = servletRequest.getContainerRequest().getSession();
        return httpSession.getMaxInactiveInterval();
    }

    public void setStaySignedIn(final Session wicketSession, boolean value) {
        wicketSession.setMetaData(STAY_SIGNED_IN, value);
    }

    public boolean getStaySignedIn(final Session wicketSession) {
        return wicketSession.getMetaData(STAY_SIGNED_IN);
    }

}
