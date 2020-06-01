/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.users;

import org.hippoecm.repository.api.HippoNodeType;

/**
 * UserDataProvider that provides system users.
 */
public class SystemUserDataProvider extends UserDataProvider {

    private static final String QUERY_SYSTEM_USER_LIST = "SELECT * " +
            " FROM " + HippoNodeType.NT_USER
            +" WHERE hipposys:system = 'true'";

    public SystemUserDataProvider() {
        super(QUERY_SYSTEM_USER_LIST);
    }
}
