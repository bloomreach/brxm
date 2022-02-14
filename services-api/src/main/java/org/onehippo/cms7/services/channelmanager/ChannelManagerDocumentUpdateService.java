/*
 * Copyright 2021-2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.channelmanager;

import javax.jcr.Session;

import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

/**
 * <p>
 *     This class:
 *     <ul>
 *         <li>Stores {@link ChannelManagerCommand}'s</li>
 *         <li>Issues request by calling {@link ChannelManagerCommand#execute(Session)}</li>
 *     </ul>
 * </p>
 */
public interface ChannelManagerDocumentUpdateService {

    /**
     * Issues request by calling {@link ChannelManagerCommand#execute(Session)} on all stored commands.
     * <p>
     *     The commands are stored per user session
     * </p>
     * @param cmsSessionContext the place where the commands are stored per user session
     * @param session the session the command should be executed on
     */
    void update(CmsSessionContext cmsSessionContext, Session session);

    /**
     * Removes the command for the current user session.
     * @param identifier Identifier of the object the command executed on
     * @param cmsSessionContext The {@link CmsSessionContext} where the command is stored.
     */
    void removeCommand(String identifier, CmsSessionContext cmsSessionContext);

    /**
     * Stores command for the the current user session
     * @param identifier Identifier of the object the command executed on
     * @param cmsSessionContext The {@link CmsSessionContext} where the command is stored.
     * @param command The command to be added
     */
    void storeCommand(String identifier, CmsSessionContext cmsSessionContext, ChannelManagerCommand command);
}
