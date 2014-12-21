/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.generator;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.forge.sitemap.components.UrlInformationProvider;
import org.onehippo.forge.sitemap.components.model.ChangeFrequency;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 */
public class DefaultUrlInformationProvider implements UrlInformationProvider {

    protected DefaultUrlInformationProvider() {}

    public static final DefaultUrlInformationProvider INSTANCE = new DefaultUrlInformationProvider();

    /**
     * Returns the hippostdpubwf:lastModificationDate property of the passed {@link HippoBean}
     */
    public Calendar getLastModified(HippoBean hippoBean) {
        return hippoBean.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE);
    }

    /**
     * Returns null for this optional field
     */
    public BigDecimal getPriority(HippoBean hippoBean) {
        return null;
    }

    /**
     * Returns null for this optional field
     */
    public ChangeFrequency getChangeFrequency(HippoBean hippoBean) {
        return null;
    }

    /**
     * Returns the canonical link to the passed {@link HippoBean} by using the default {@link HstLinkCreator} in the
     * passed {@link HstRequestContext}.
     */
     public String getLoc(HippoBean hippoBean, HstRequestContext requestContext) {
        return getLoc(hippoBean, requestContext, requestContext.getResolvedMount().getMount());
    }

    public String getLoc(HippoBean hippoBean, HstRequestContext requestContext, Mount mount) {
        HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        return linkCreator.create(hippoBean.getNode(), mount).toUrlForm(requestContext, true);
    }

    /**
     * By default, all matched documents are included in the site map
     * @param hippoBean the current document to validate
     * @return <code>true</code>
     */
    public boolean includeDocumentInSiteMap(HippoBean hippoBean) {
        return true;
    }

    /**
     * By default, all children of matched documents are included in the site map
     * @param hippoBean the current document to validate
     * @return <code>true</code>
     */
    public boolean includeChildrenInSiteMap(HippoBean hippoBean) {
        return true;
    }
}
