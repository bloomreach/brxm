/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import java.util.Set;

import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.jcr.pool.SimpleCredentialsFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class SimpleCredentialsFactoryBean extends AbstractFactoryBean<SimpleCredentials> {

    private String userId;
    private String separator;
    private String poolName;
    private char[] password;
    private Set<String> hstJmvEnabledUsers;

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setSeparator(final String separator) {
        this.separator = separator;
    }

    public void setPoolName(final String poolName) {
        this.poolName = poolName;
    }

    public void setPassword(final char[] password) {
        this.password = password;
    }

    public void setHstJmvEnabledUsers(final Set<String> hstJmvEnabledUsers) {
        this.hstJmvEnabledUsers = hstJmvEnabledUsers;
    }

    @Override
    public Class<?> getObjectType() {
        return SimpleCredentials.class;
    }

    @Override
    protected SimpleCredentials createInstance() throws Exception {
        return SimpleCredentialsFactory.createInstance(userId, password, separator, poolName , hstJmvEnabledUsers);
    }
}