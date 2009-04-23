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
package org.hippoecm.hst.core.linking;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * HstLinkCreator implementations must be able to create a <code>{@link HstLink}</code> for the methods
 * <ul>
 *  <li>{@link #create(HstSiteMapItem)}</li>
 *  <li>{@link #create(HstSite, String)}</li>
 *  <li>{@link #create(Node, ResolvedSiteMapItem)}</li>
 *  <li>{@link #create(String, ResolvedSiteMapItem)}</li>
 * </ul>
 * 
 * A specific implementation must be available on the <code>HstRequestContext</code> through the 
 * {@link org.hippoecm.hst.core.request.HstRequestContext#getHstLinkCreator()}.
 *
 */
public interface HstLinkCreator {


    /**
     * Rewrite a jcr uuid to a HstLink wrt its current ResolvedSiteMapItem. 
     * @param uuid the uuid of the node that must be used to link to
     * @param session jcr session 
     * @param resolvedSiteMapItem
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String uuid, Session session, ResolvedSiteMapItem resolvedSiteMapItem);
    
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem
     * @param node
     * @param resolvedSiteMapItem
     * @return HstLink 
     */
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    

    /**
     * 
     * @param node
     * @param hstRequestContext
     * @return
     */
    HstLink create(Node node, HstRequestContext hstRequestContext);
    
    /**
     * 
     * @param bean
     * @param hstRequestContext
     * @return
     */
    HstLink create(HippoBean bean, HstRequestContext hstRequestContext);
    
    
    /**
     * For creating a link from a HstSiteMapItem to a HstSiteMapItem with toSiteMapItemId within the same Site
     * @param toSiteMapItemId
     * @param resolvedSiteMapItem
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String toSiteMapItemId, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * Regardless the current context, create a HstLink to the HstSiteMapItem that you use as argument. This is only possible if the sitemap item does not
     * contain any ancestor including itself with a wildcard, because the link is ambiguous in that case. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param toHstSiteMapItem the {@link HstSiteMapItem} to link to
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(HstSiteMapItem toHstSiteMapItem);
    
    /**
     * Regardless the current context, create a HstLink to the siteMapPath that you use as argument. 
     * @param siteMapPath the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String siteMapPath, HstSite hstSite);
    
    /**
     * create a link to a HstSiteMapItem with id <code>toSiteMapItemId</code> that belongs to <code>HstSite</code> hstSite.
     * Note that the HstSite can be a different one then the current, possibly resulting in a cross-domain (host) link. 
     * A <code>HstLink</code> can only be created unambiguous if the <code>HstSiteMapItem</code> belonging to toSiteMapItemId does not
     * contain any ancestor including itself with a wildcard. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param hstSite the HstSite the toSiteMapItemId should be in
     * @param toSiteMapItemId the id of the SiteMapItem ({@link HstSiteMapItem#getId()})
     * @return HstLink
     */
    HstLink create(HstSite hstSite, String toSiteMapItemId);
}
