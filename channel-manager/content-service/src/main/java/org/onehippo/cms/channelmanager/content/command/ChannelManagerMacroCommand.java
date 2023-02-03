/*
 * Copyright 2021-2023 Bloomreach (https://www.bloomreach.com)
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
package org.onehippo.cms.channelmanager.content.command;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Session;

import org.onehippo.cms7.services.channelmanager.ChannelManagerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerMacroCommand implements ChannelManagerCommand {

    private final List<ChannelManagerCommand> channelManagerCommandList = new CopyOnWriteArrayList<>();
    private final Logger log = LoggerFactory.getLogger(ChannelManagerMacroCommand.class);

    public void addCommand(ChannelManagerCommand command){
        channelManagerCommandList.add(command);
    }

    @Override
    public void execute(final Session previewCmsUserSession) {
        synchronized (previewCmsUserSession) {
            log.debug("Executing macrocommand : {} on previewCmsUserSession {}", this, previewCmsUserSession);
            for (ChannelManagerCommand command : channelManagerCommandList) {
                log.debug("Executing command: {} on previewCmsUserSession {},", command, previewCmsUserSession);
                try {
                    command.execute(previewCmsUserSession);
                } catch (Exception e) {
                    log.warn("Something went wrong during execution of {} on previewCmsUserSession: { userId: {} }", this,
                            previewCmsUserSession.getUserID(), e);
                    log.warn("Stop executing other commands for this macrocommand");
                    break;
                }
            }
        }

    }
}
