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

package org.onehippo.cms7.essentials.dashboard.restservices;

/**
* @version "$Id$"
*/
public  class ValidBean {
    private String beanName;
    private String beanPackage;
    private String beanPath;
    private String fullQualifiedName;
    private String fullQualifiedResourceName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(final String beanName) {
        this.beanName = beanName;
    }

    public String getBeanPackage() {
        return beanPackage;
    }

    public void setBeanPackage(final String beanPackage) {
        this.beanPackage = beanPackage;
    }

    public String getBeanPath() {
        return beanPath;
    }

    public void setBeanPath(final String beanPath) {
        this.beanPath = beanPath;
    }

    public String getFullQualifiedName() {
        return fullQualifiedName;
    }

    public void setFullQualifiedName(final String fullQualifiedName) {
        this.fullQualifiedName = fullQualifiedName;
    }

    public String getFullQualifiedResourceName() {
        return fullQualifiedResourceName;
    }

    public void setFullQualifiedResourceName(final String fullQualifiedResourceName) {
        this.fullQualifiedResourceName = fullQualifiedResourceName;
    }
}
