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
import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.channelmanager.ChannelManagerCommand;
import org.slf4j.Logger;

import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;

public class UpdateEditableFieldChannelManagerCommand implements ChannelManagerCommand {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UpdateEditableFieldChannelManagerCommand.class);
    private final String uuid;
    private final FieldPath fieldPath;
    private final List<FieldValue> fieldValues;
    private final List<FieldType> fieldTypes;
    private final Locale locale;
    private final TimeZone timeZone;

    UpdateEditableFieldChannelManagerCommand(String uuid, FieldPath fieldPath, List<FieldValue> fieldValues,
                                             List<FieldType> fieldTypes, Locale locale, TimeZone timeZone) {
        this.uuid = uuid;
        this.fieldPath = fieldPath;
        this.fieldValues = fieldValues;
        this.fieldTypes = fieldTypes;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    public static UpdateEditableFieldChannelManagerCommandBuilder builder() {
        return new UpdateEditableFieldChannelManagerCommandBuilder();
    }

    @Override
    public void execute(final Session session) {
        log.debug("Execute {} on session: { {}, userId: {} }", this, session, session.getUserID());
        final Node handle = getHandle(uuid, session);
        final Node variantNode = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED)
                .orElseThrow(() -> new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST)));
        final CompoundContext documentContext = new CompoundContext(variantNode, variantNode, locale, timeZone);
        FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fieldTypes, documentContext);
    }

    public String toString() {
        return "UpdateEditableFieldChannelManagerCommand(uuid=" + this.uuid + ", fieldPath=" + this.fieldPath + ", locale=" + this.locale + ", timeZone=" + this.timeZone + ")";
    }

    public static class UpdateEditableFieldChannelManagerCommandBuilder {
        private String uuid;
        private FieldPath fieldPath;
        private List<FieldValue> fieldValues;
        private List<FieldType> fieldTypes;
        private Locale locale;
        private TimeZone timeZone;

        UpdateEditableFieldChannelManagerCommandBuilder() {
        }

        public UpdateEditableFieldChannelManagerCommandBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public UpdateEditableFieldChannelManagerCommandBuilder fieldPath(FieldPath fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }

        public UpdateEditableFieldChannelManagerCommandBuilder fieldValues(List<FieldValue> fieldValues) {
            this.fieldValues = fieldValues;
            return this;
        }

        public UpdateEditableFieldChannelManagerCommandBuilder fieldTypes(List<FieldType> fieldTypes) {
            this.fieldTypes = fieldTypes;
            return this;
        }

        public UpdateEditableFieldChannelManagerCommandBuilder locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public UpdateEditableFieldChannelManagerCommandBuilder timeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public UpdateEditableFieldChannelManagerCommand build() {
            return new UpdateEditableFieldChannelManagerCommand(uuid, fieldPath, fieldValues, fieldTypes, locale,
                    timeZone);
        }

        public String toString() {
            return "UpdateEditableFieldChannelManagerCommand.UpdateEditableFieldChannelManagerCommandBuilder(uuid=" + this.uuid + ", fieldPath=" + this.fieldPath + ", fieldValues=" + this.fieldValues + ", fieldTypes=" + this.fieldTypes + ", locale=" + this.locale + ", timeZone=" + this.timeZone + ")";
        }
    }
}
