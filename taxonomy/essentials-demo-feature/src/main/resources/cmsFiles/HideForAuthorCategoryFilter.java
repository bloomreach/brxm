/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.demo;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;
import org.onehippo.taxonomy.plugin.model.JcrCategory;

/**
 * Demo category filter, hiding all categories except first level if the user is part of the author group.
 * <p>
 * This filter is not configured by default, you can do so in a String property 'taxonomy.category.filters' on the
 * taxonomy service at /hippo:configuration/hippo:frontend/cms/cms-services/taxonomyService
 */
public class HideForAuthorCategoryFilter implements JcrCategoryFilter {
    @Override
    public boolean apply(final JcrCategory category, final HippoSession session) {
        try {
            final User user = session.getUser();

            // user 'author'..
            if (user.getId().equals("author")) {
                // no category parent means it's a top level category
                if (category.getParent() != null) {
                    return false;
                }
            }

            // ..or in author group
            // NB for the memberships to show, https://issues.onehippo.com/browse/REPO-1081 has to be resolved or
            //    the domain rule attachments in that issue have to be imported into the local repository
            for (final Group group : user.getMemberships()) {
                if (group.getId().equals("author")) {
                    // no category parent means it's a top level category
                    if (category.getParent() != null) {
                        return false;
                    }
                }
            }
        } catch (RepositoryException e) {
            // it's only a demo
            e.printStackTrace();
        }

        return true;
    }
}
