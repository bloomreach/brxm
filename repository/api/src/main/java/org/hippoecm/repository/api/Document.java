/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

import java.io.Serializable;

public class Document implements Serializable, Cloneable {
    private transient Document isCloned = null;
    private String identity = null;

    public Document() {
    }

    public Document(String identity) {
        this.identity = identity;
    }

    public Object clone() throws CloneNotSupportedException {
        Document document = (Document) super.clone();
        document.setCloned(this);
        document.identity = null;
        return document;
    }

    private void setCloned(Document document) {
        isCloned = document;
    }

    /**
     * Obtain the identity, if known at this point, of a document.  The
     * identity of a Document is the identity of the primary javax.jcr.Node
     * used in persisting the data of the document.</p>
     * A Document returned for example by a workflow step can be accessed
     * using:
     * <code>Node node = session.getNodeByUUID(document.getIdentity());</code>
     *
     * @returns a string containing the UUID of the Node representing the Document.
     * or <code>null</code> if not available.
     */
    public final String getIdentity() {
        return identity;
    }

    /**
     * THIS CALL IS NOT PART OF THE PUBLIC API OF HIPPO ECM.<p/>
     * In no cirtumstance should this call be used.
     */
    public final void setIdentity(String uuid) {
        identity = uuid;
    }

    /**
     * This call is not useful for extension programmers, and should normally
     * not be used.
     */
    public Document isCloned() {
        return isCloned;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Document[");
        if (identity != null) {
            sb.append("uuid=");
            sb.append(identity);
        } else if (isCloned != null) {
            sb.append("cloned=");
            sb.append(isCloned.identity);
        } else {
            sb.append("new");
        }
        sb.append("]");
        return new String(sb);
    }
}
