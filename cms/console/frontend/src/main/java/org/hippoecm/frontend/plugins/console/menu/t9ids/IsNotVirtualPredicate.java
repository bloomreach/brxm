/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.t9ids;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.predicate.Predicate;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.repository.util.JcrUtils;

class IsNotVirtualPredicate implements Predicate {

    static final IsNotVirtualPredicate INSTANCE = new IsNotVirtualPredicate();

    private IsNotVirtualPredicate() {
    }

    @Override
    public boolean evaluate(final Object object) {
        if (object instanceof Node) {
            final Node node = (Node) object;
            try {
                return !JcrHelper.isVirtualNode(node);
            } catch (RepositoryException e) {
                throw new IllegalStateException("Cannot determine if node "
                        + JcrUtils.getNodePathQuietly(node) + " is real or virtual");
            }
        }
        return true;
    }

}
