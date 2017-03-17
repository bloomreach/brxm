/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.restapi;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.restapi.content.linking.RestApiLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.contenttype.ContentTypes;

public interface ResourceContext {
    HstRequestContext getRequestContext();
    ContentTypes getContentTypes();
    NodeVisitor getVisitor(Node node) throws RepositoryException;
    NodeVisitor getPrimaryNodeTypeVisitor(Node node) throws RepositoryException;
    RestApiLinkCreator getRestApiLinkCreator();
    List<String> getIncludedAttributes();
}
