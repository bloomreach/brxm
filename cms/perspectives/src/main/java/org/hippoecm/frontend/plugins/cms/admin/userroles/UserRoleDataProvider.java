/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.userroles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.UserRole;

public class UserRoleDataProvider extends SortableDataProvider<UserRole, String> {

    private static final Logger log = LoggerFactory.getLogger(UserRoleDataProvider.class);

    private List<UserRole> userRoles = new ArrayList<>();

    public UserRoleDataProvider() {
        setSort("name", SortOrder.ASCENDING);
    }

    @Override
    public Iterator<UserRole> iterator(long first, long count) {
        userRoles.sort((userRole1, userRole2) -> {
            final int direction = getSort().isAscending() ? 1 : -1;
            return direction * userRole1.getName().compareTo(userRole2.getName());
        });

        final int endIndex = (int) Math.min(first + count, userRoles.size());
        return userRoles.subList((int) first, endIndex).iterator();
    }

    @Override
    public IModel<UserRole> model(UserRole userRole) {
        return new DetachableUserRole(userRole);
    }

    @Override
    public long size() {
        return getUserRoleList().size();
    }

    public List<UserRole> getUserRoleList() {
        userRoles.clear();
        userRoles.addAll(SecurityManagerHelper.getUserRolesProvider().getRoles());
        return userRoles;
    }
}
