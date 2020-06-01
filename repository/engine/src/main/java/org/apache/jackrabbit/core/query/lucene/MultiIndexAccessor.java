/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.lucene.document.Document;

public class MultiIndexAccessor {

    public static void removeAllDocuments(final MultiIndex multiIndex, final NodeId id) throws IOException {
        multiIndex.removeAllDocuments(id);
    }

    public static Document createDocument(final MultiIndex multiIndex, final NodeState node) throws RepositoryException {
        return multiIndex.createDocument(node);
    }

    public static void addDocument(final MultiIndex multiIndex, final Document d) throws IOException {
        multiIndex.addDocument(d);
    }
}
