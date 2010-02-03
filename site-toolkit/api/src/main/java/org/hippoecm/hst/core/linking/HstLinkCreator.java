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

import java.util.List;

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
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem and preferredItem. The link is tried to be rewritten to 
     * one of the descendant HstSiteMapItem's or self of the preferredItem. When there cannot be created an HstLink to a descendant HstSiteMapItem 
     * or self, then:
     * 
     * <ol>
     *  <li>when <code>fallback = true</code>, a fallback to {@link #create(Node, ResolvedSiteMapItem)} is done</li>
     *  <li>when <code>fallback = false</code>, dependent on the implementation some error HstLink or <code>null</code> can be returned</li>
     * </ol>
     *  
     * @param node
     * @param resolvedSiteMapItem
     * @param preferredItem
     * @param fallback 
      * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem, boolean fallback);
    
    /**
     * This creates a canonical HstLink: regardless the context, one and the same jcr Node is garantueed to return the same HstLink. This is
     * useful when showing one and the same content via multiple urls, for example in faceted navigation. Search engines can better index your
     * website when defining a canonical location for duplicate contents: See 
     * <a href="http://googlewebmastercentral.blogspot.com/2009/02/specify-your-canonical.html">specify-your-canonical</a> for more info on this subject.
     * 
     * @param node
     * @param resolvedSiteMapItem
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem);
    
    /**
     * @see {@link #createCanonical(Node, ResolvedSiteMapItem)}.
     * When specifying a preferredItem, we try to create a canonical link wrt this preferredItem. If the link cannot be created for this preferredItem,
     * a fallback to {@link #createCanonical(Node, ResolvedSiteMapItem)} without preferredItem is done.
     * 
     * @param node
     * @param resolvedSiteMapItem
     * @param preferredItem if <code>null</code>, a fallback to {@link #createCanonical(Node, ResolvedSiteMapItem)} is done
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, ResolvedSiteMapItem resolvedSiteMapItem, HstSiteMapItem preferredItem);
    
    
    
    /**
     * 
     * @param node
     * @param hstRequestContext
     * @return
     * @deprecated  Use {@link  #create(Node, ResolvedSiteMapItem)} 
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
     * For creating a link from a HstSiteMapItem to a HstSiteMapItem with toSiteMapItemId
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
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, HstSite hstSite);
    
    /**
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param hstSite the HstSite the siteMapPath should be in
     * @param containerResource whether it is a static link, for example for css/js
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, HstSite hstSite, boolean containerResource);
    
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
    
    /**
     * Binaries frequently have a different linkrewriting mechanism. If this method returns <code>true</code> the location is a
     * binary location. 
     * @param path
     * @return <code>true</code> when the path points to a binary location
     */
    boolean isBinaryLocation(String path);
    
    /**
     * @return The prefix that is used for binary locations. The returned binaries prefix is relative to <code>/</code> and 
     * does not include the <code>/</code> itself. If no binaries prefix is configured, <code>""</code> will be returned
     */ 
    String getBinariesPrefix();
    
    /**
     * @return the list of location resolvers, primarily used for resolving custom binary locations 
     */
    List<LocationResolver> getLocationResolvers();
}
