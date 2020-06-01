/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.channelmanager.security;

import java.security.Principal;

import javax.jcr.Session;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Very simple security model for the hst configuration. Currently only supported is checking whether the user is in
 * role {@link #CHANNEL_MANAGER_ADMIN_ROLE} or {@link #CHANNEL_WEBMASTER_ROLE}
 */
public interface SecurityModel {

    // at this moment the only supported functional role
    String CHANNEL_MANAGER_ADMIN_ROLE = "ChannelManagerAdmin";
    String CHANNEL_WEBMASTER_ROLE = "ChannelWebmaster";

    /**
     * @return return user {@link Principal} of currently active user
     */
    Principal getUserPrincipal(Session session);

    /**
     * @return {@code true} when the user for the current {@code context} is in role {@code functionalRole} where the
     * {@code functionalRole} can be either {@link #CHANNEL_MANAGER_ADMIN_ROLE} or {@link #CHANNEL_WEBMASTER_ROLE}
     */
    boolean isUserInRole(Session session, String functionalRole);
}
