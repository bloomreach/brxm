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

import java.util.List;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.NodeFieldServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms7.services.channelmanager.ChannelManagerCommand;
import org.slf4j.Logger;

import static org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl.getAbsolutePath;
import static org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl.getUnpublished;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;

public class RemoveNodeFieldCommand implements ChannelManagerCommand {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RemoveNodeFieldCommand.class);
    private final String uuid;
    private final FieldPath fieldPath;
    private final List<FieldType> fieldTypes;

    RemoveNodeFieldCommand(String uuid, FieldPath fieldPath, List<FieldType> fieldTypes) {
        this.uuid = uuid;
        this.fieldPath = fieldPath;
        this.fieldTypes = fieldTypes;
    }

    public static RemoveNodeFieldCommandBuilder builder() {
        return new RemoveNodeFieldCommandBuilder();
    }

    @Override
    public void execute(final Session previewSession) {
        log.debug("Execute {} on previewSession: { {}, userId: {} }", this, previewSession, previewSession.getUserID());
        final String variantAbsolutePath =  getAbsolutePath(getUnpublished(getHandle(uuid, previewSession)));
        new NodeFieldServiceImpl(previewSession).removeNodeField(variantAbsolutePath, fieldPath, fieldTypes);
    }

    public String toString() {
        return "RemoveNodeFieldCommand(uuid=" + this.uuid + ", fieldPath=" + this.fieldPath + ")";
    }

    public static class RemoveNodeFieldCommandBuilder {
        private String uuid;
        private FieldPath fieldPath;
        private List<FieldType> fieldTypes;

        RemoveNodeFieldCommandBuilder() {
        }

        public RemoveNodeFieldCommandBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public RemoveNodeFieldCommandBuilder fieldPath(FieldPath fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }

        public RemoveNodeFieldCommandBuilder fieldTypes(List<FieldType> fieldTypes) {
            this.fieldTypes = fieldTypes;
            return this;
        }

        public RemoveNodeFieldCommand build() {
            return new RemoveNodeFieldCommand(uuid, fieldPath, fieldTypes);
        }

        public String toString() {
            return "RemoveNodeFieldCommand.RemoveNodeFieldCommandBuilder(uuid=" + this.uuid + ", fieldPath=" + this.fieldPath + ", fieldTypes=" + this.fieldTypes + ")";
        }
    }
}
