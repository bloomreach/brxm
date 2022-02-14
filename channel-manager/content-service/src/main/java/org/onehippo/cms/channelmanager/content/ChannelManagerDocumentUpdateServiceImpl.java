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
package org.onehippo.cms.channelmanager.content;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.command.ChannelManagerMacroCommand;
import org.onehippo.cms7.services.channelmanager.ChannelManagerCommand;
import org.onehippo.cms7.services.channelmanager.ChannelManagerDocumentUpdateService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerDocumentUpdateServiceImpl implements ChannelManagerDocumentUpdateService {

    private static final String COMMAND_CMS_SESSION_CONTEXT_PAYLOAD_KEY = ChannelManagerDocumentUpdateService.class.getName();
    private static final Logger log = LoggerFactory.getLogger(ChannelManagerDocumentUpdateServiceImpl.class);

    @Override
    public void update(final CmsSessionContext cmsSessionContext, final Session session) {
        log.debug("Updating session : { {}, userId: {}}", session, session.getUserID());
        getCommands(cmsSessionContext).values().forEach(command-> command.execute(session));
    }

    @Override
    public void removeCommand(String identifier, CmsSessionContext cmsSessionContext){
        log.debug("Remove macrocommand for node: { id: {}} for cmsSessionContext: {}"
                , identifier, cmsSessionContext);
        getCommands(cmsSessionContext).remove(identifier);
    }

    @Override
    public void storeCommand(final String identifier, final CmsSessionContext cmsSessionContext, final ChannelManagerCommand command) {
        final Map<String, ChannelManagerMacroCommand> commands = getCommands(cmsSessionContext);
        commands.computeIfAbsent(identifier, k -> new ChannelManagerMacroCommand());
        log.debug("Add command: {} for node: { id: {} } for cmsSessionContext: {}",
                command, identifier, cmsSessionContext);
        commands.get(identifier).addCommand(command);
    }

    /**
     * @return map of Map<String, ComponentContent> on cms session context and empty Map if missing
     */
    public Map<String, ChannelManagerMacroCommand> getCommands(CmsSessionContext cmsSessionContext) {

        final Map<String, Serializable> contextPayload = getContextPayload(cmsSessionContext);
        return (Map<String, ChannelManagerMacroCommand>) contextPayload
                .computeIfAbsent(COMMAND_CMS_SESSION_CONTEXT_PAYLOAD_KEY, k -> new ConcurrentHashMap<>());
    }

    private Map<String, Serializable> getContextPayload(CmsSessionContext cmsSessionContext){
        final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();
        if ( contextPayload == null ){
            log.warn("ContextPayload is null, returning an empty map." +
                    "Please make sure it's initialized during initialization of the CmsSessionContext !");
            return Collections.emptyMap();
        }
        return contextPayload;
    }




}
