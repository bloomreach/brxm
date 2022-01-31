/*
 * Copyright 2021-2022 Bloomreach
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
package org.onehippo.cms.channelmanager.content.document;

import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.DOES_NOT_EXIST;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.SERVER_ERROR;

public class NodeFieldRemoval {

    private final String documentPath;
    private final FieldPath fieldPath;
    private final List<FieldType> fields;
    private final Session session;
    private static final Logger log = LoggerFactory.getLogger(NodeFieldReorder.class);

    public NodeFieldRemoval(final String documentPath, final FieldPath fieldPath, final List<FieldType> fields,
                            final Session session) {
        this.documentPath = documentPath;
        this.fieldPath = fieldPath;
        this.fields = fields;
        this.session = session;
    }

    @Override
    public String toString() {
        return "NodeFieldRemoval{" + "documentPath='" + documentPath + '\'' + ", fieldPath=" + fieldPath + ", userId=" + session.getUserID() + '}';
    }

    public void remove(){
        NodeFieldServiceImpl.getFieldType(fieldPath, fields);

        final String nodeFieldPath = documentPath + SEPARATOR + fieldPath;
        try {
            session.removeItem(nodeFieldPath);
        } catch (final PathNotFoundException e) {
            throw new NotFoundException(new ErrorInfo(DOES_NOT_EXIST, "field", fieldPath.toString()));
        } catch (final RepositoryException e) {
            log.error("Failed to remove field '{}' from document '{}'", fieldPath, nodeFieldPath, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }
}
