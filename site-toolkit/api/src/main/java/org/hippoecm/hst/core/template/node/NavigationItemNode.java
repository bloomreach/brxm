/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.template.node;

import javax.jcr.RepositoryException;

public interface NavigationItemNode extends TemplateNode {
    public static final String SITEMAPITEM_ATTRIBUTE_PROPERTY = "hst:sitemapitem";
    public static final String TITLE_ATTRIBUTE_PROPERTY = "hst:title";

    String getSiteMapItemValue() throws RepositoryException;

    String getTitleValue() throws RepositoryException;

    NodeList<NavigationItemNode> getNavigationSubItems() throws RepositoryException;

    String getValueByPropertyName(String propertyName) throws RepositoryException;
}