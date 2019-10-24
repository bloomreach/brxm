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

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;

import com.bloomreach.xm.repository.security.UserRole;

public final class DetachableUserRole extends LoadableDetachableModel<UserRole> {

    private final String name;

    public DetachableUserRole(final UserRole userRole) {
        this(userRole.getName());
        setObject(userRole);
    }

    public DetachableUserRole(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (name == null) {
            return super.hashCode();
        }
        return name.hashCode();
    }

    /**
     * used for dataview with ReuseIfModelsEqualStrategy item reuse strategy
     *
     * @see org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof DetachableUserRole && name.equals(((DetachableUserRole) obj).name);
    }

    /**
     * @see LoadableDetachableModel#load()
     */
    @Override
    protected UserRole load() {
        if (name == null) {
            return null;
        }
        return SecurityManagerHelper.getUserRolesProvider().getRole(name);
    }
}
