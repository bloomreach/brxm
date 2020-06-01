/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.linking.RewriteContext;
import org.hippoecm.hst.core.linking.RewriteContextResolver;
import org.hippoecm.hst.core.request.HstRequestContext;

public class DefaultRewriteContextResolver implements RewriteContextResolver {

    @Override
    public RewriteContext resolve(final Node node,
                                  final Mount mount,
                                  final HstRequestContext context,
                                  final boolean canonical,
                                  final boolean navigationStateful) {
        try {
           return new RewriteContext(node.getPath(), mount, canonical, navigationStateful);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }
}
