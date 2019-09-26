/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services.channel;

import javax.jcr.security.Privilege;

public class ChannelManagerPrivileges {

    public final static String CHANNEL_ADMIN_PRIVILEGE_NAME = "hippo:channel-admin";

    public final static String CHANNEL_WEBMASTER_PRIVILEGE_NAME = "hippo:channel-webmaster";

    public final static String CHANNEL_WEB_VIEWER_PRIVILEGE_NAME = "hippo:channel-webviewer";


    public static Privilege[] createPrivileges(final String... names) {

        final Privilege[] privileges = new Privilege[names.length];

        for (int i = 0; i < names.length; i++) {
            final String name = names[i];

            privileges[i] = new Privilege() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public boolean isAbstract() {
                    return false;
                }

                @Override
                public boolean isAggregate() {
                    return false;
                }

                @Override
                public Privilege[] getDeclaredAggregatePrivileges() {
                    return new Privilege[0];
                }

                @Override
                public Privilege[] getAggregatePrivileges() {
                    return new Privilege[0];
                }
            };
        }
        return privileges;
    }
}
