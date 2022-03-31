/*
 * Copyright 2021-2022 Bloomreach Inc.
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

import org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl;
import org.onehippo.cms.channelmanager.content.document.NodeFieldServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms7.services.channelmanager.ChannelManagerCommand;
import org.slf4j.Logger;

import static org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl.getAbsolutePath;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;

public class AddNodeFieldCommand implements ChannelManagerCommand {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AddNodeFieldCommand.class);
    private final String uuid;
    private final FieldPath fieldPath;
    private final String type;
    private final List<FieldType> fieldTypes;

    AddNodeFieldCommand(String uuid, FieldPath fieldPath, String type, List<FieldType> fieldTypes) {
        this.uuid = uuid;
        this.fieldPath = fieldPath;
        this.type = type;
        this.fieldTypes = fieldTypes;
    }

    public static AddNodeFieldCommandBuilder builder() {
        return new AddNodeFieldCommandBuilder();
    }

    @Override
    public void execute(final Session previewCmsUserSession) {
        log.debug("Execute {} on session: { {}, userId: {} }", this, previewCmsUserSession, previewCmsUserSession.getUserID());
        final String variantAbsolutePath = getAbsolutePath(
                DocumentsServiceImpl.getUnpublished(getHandle(uuid, previewCmsUserSession)));
        new NodeFieldServiceImpl(previewCmsUserSession).addNodeField(variantAbsolutePath, fieldPath, fieldTypes, type);
    }

    public String toString() {
        return "AddNodeFieldCommand{uuid='" + this.uuid + "', fieldPath='" + this.fieldPath + "', type='" + this.type + "'}";
    }

    public static class AddNodeFieldCommandBuilder {
        private String uuid;
        private FieldPath fieldPath;
        private String type;
        private List<FieldType> fieldTypes;

        AddNodeFieldCommandBuilder() {
        }

        public AddNodeFieldCommandBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public AddNodeFieldCommandBuilder fieldPath(FieldPath fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }

        public AddNodeFieldCommandBuilder type(String type) {
            this.type = type;
            return this;
        }

        public AddNodeFieldCommandBuilder fieldTypes(List<FieldType> fieldTypes) {
            this.fieldTypes = fieldTypes;
            return this;
        }

        public AddNodeFieldCommand build() {
            return new AddNodeFieldCommand(uuid, fieldPath, type, fieldTypes);
        }

        public String toString() {
            return "AddNodeFieldCommand.AddNodeFieldCommandBuilder{uuid='" + this.uuid + "', fieldPath='" + this.fieldPath
                    + "', type='" + this.type + "', fieldTypes='" + this.fieldTypes + "'}";
        }
    }
}
