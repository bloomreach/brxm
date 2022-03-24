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
package org.onehippo.cms.channelmanager.content.command;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.NodeFieldServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms7.services.channelmanager.ChannelManagerCommand;
import org.slf4j.Logger;

import static org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl.getAbsolutePath;
import static org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl.getUnpublished;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;


public class ReorderNodeFieldCommand implements ChannelManagerCommand {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ReorderNodeFieldCommand.class);
    private final String uuid;
    private final FieldPath fieldPath;
    private final int position;

    ReorderNodeFieldCommand(String uuid, FieldPath fieldPath, int position) {
        this.uuid = uuid;
        this.fieldPath = fieldPath;
        this.position = position;
    }

    public static ReorderNodeFieldCommandBuilder builder() {
        return new ReorderNodeFieldCommandBuilder();
    }


    @Override
    public void execute(final Session session) {
        log.debug("Execute {} on session: { {}, userId: {} }", this, session, session.getUserID());
        final String variantAbsolutePath = getAbsolutePath(getUnpublished(getHandle(uuid, session)));
        new NodeFieldServiceImpl(session).reorderNodeField(variantAbsolutePath, fieldPath, position);
    }

    public String toString() {
        return "ReorderNodeFieldCommand(uuid=" + this.uuid + ", fieldPath=" + this.fieldPath + ", position=" + this.position + ")";
    }

    public static class ReorderNodeFieldCommandBuilder {
        private String uuid;
        private FieldPath fieldPath;
        private int position;

        ReorderNodeFieldCommandBuilder() {
        }

        public ReorderNodeFieldCommandBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public ReorderNodeFieldCommandBuilder fieldPath(FieldPath fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }

        public ReorderNodeFieldCommandBuilder position(int position) {
            this.position = position;
            return this;
        }

        public ReorderNodeFieldCommand build() {
            return new ReorderNodeFieldCommand(uuid, fieldPath, position);
        }

        public String toString() {
            return "ReorderNodeFieldCommand.ReorderNodeFieldCommandBuilder(uuid=" + this.uuid + ", fieldPath=" + this.fieldPath + ", position=" + this.position + ")";
        }
    }
}
