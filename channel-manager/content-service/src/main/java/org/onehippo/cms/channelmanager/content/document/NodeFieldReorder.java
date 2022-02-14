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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.util.FieldPath.SEPARATOR;
import static org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason.SERVER_ERROR;

public class NodeFieldReorder {

    private final String documentPath;
    private final FieldPath fieldPath;
    private final int position;
    private final Session session;
    private static final Logger log = LoggerFactory.getLogger(NodeFieldReorder.class);

    public NodeFieldReorder(final String documentPath, final FieldPath fieldPath, final int position,
                            final Session session) {
        this.documentPath = documentPath;
        this.fieldPath = fieldPath;
        this.position = position;
        this.session = session;
    }

    @Override
    public String toString() {
        return "NodeFieldReorder{" + "documentPath='" + documentPath + '\'' + ", fieldPath=" + fieldPath
                + ", position=" + position + ", userId=" + session.getUserID() + '}';
    }

    public void reorder(){
        final String nodeFieldPath = documentPath + SEPARATOR + fieldPath;
        try {
            final Node fieldNode = session.getNode(nodeFieldPath);
            final Node parentNode = fieldNode.getParent();
            final String fieldName = fieldPath.getLastSegmentName();
            final long numberOfFields = parentNode.getNodes(fieldName).getSize();

            if (position > numberOfFields) {
                log.error("Failed to re-order field '{}', new position '{}' is out of bounds", fieldPath, position);
                throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR, "order", "out-of-bounds"));
            }

            final String relativeFieldPath = fieldPath.getLastSegment();
            if (numberOfFields == position) {
                // move to last position
                parentNode.orderBefore(relativeFieldPath, null);
            } else if (fieldNode.getIndex() > position) {
                // move field up
                parentNode.orderBefore(relativeFieldPath, fieldName + "[" + position + "]");
            } else {
                // move field down
                parentNode.orderBefore(relativeFieldPath, fieldName + "[" + (position + 1) + "]");
            }

        } catch (final RepositoryException e) {
            log.error("Failed to re-order field '{}' to position '{}'", nodeFieldPath, position, e);
            throw new InternalServerErrorException(new ErrorInfo(SERVER_ERROR));
        }
    }
}
