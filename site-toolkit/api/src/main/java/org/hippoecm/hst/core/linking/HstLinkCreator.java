/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HstLinkCreator interface for creating {@link HstLink}'s
 */
public interface HstLinkCreator {
    
    /**
     * Rewrite a jcr uuid to a HstLink wrt its current ResolvedSiteMapItem. 
     * @param uuid the uuid of the node that must be used to link to
     * @param session jcr session 
     * @param requestContext the HstRequestContext
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String uuid, Session session, HstRequestContext requestContext);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current ResolvedSiteMapItem
     * @param node
     * @param requestContext the HstRequestContext
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, HstRequestContext requestContext);
    
   
    /**
     * Rewrite a jcr Node to a HstLink wrt its current HstRequestContext and preferredItem. When <code>preferredItem</code> is not <code>null</code>, the link is tried to be rewritten to 
     * one of the descendants (including itself) of the preferred {@link HstSiteMapItem}. When <code>preferredItem</code> is <code>null</code>, a link is created against the entire sitemap item tree. When there cannot be created an HstLink to a descendant HstSiteMapItem 
     * or self, then:
     * 
     * <ol>
     *  <li>when <code>fallback = true</code>, a fallback to {@link #create(Node, HstRequestContext)} is done</li>
     *  <li>when <code>fallback = false</code>, dependent on the implementation some error HstLink or <code>null</code> can be returned</li>
     * </ol>
     * <p>
     * This method returns an {@link HstLink} that takes the current URL into account, but does compute the link with respect to the physical (canonical) location
     * of the jcr Node. <b>If</b> you need a {@link HstLink} within the context of the possible virtual jcr Node (for example in case of in context showing documents in faceted navigation), use
     * {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean, boolean)} with <code>navigationStateful = true</code>
     * </p>
     * @see #create(Node, HstRequestContext, HstSiteMapItem, boolean, boolean) 
     * @param node the jcr node
     * @param requestContext the HstRequestContext
     * @param preferredItem if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
      * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem, boolean fallback);
    
   
    /**
     * <p>
     * This method creates the same {@link HstLink} as {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean)} when <code>navigationStateful = false</code>. When <code>navigationStateful = true</code>, 
     * the link that is created is with respect to the jcr Node <code>node</code>, even if this node is a virtual location. This is different then {@link #create(Node, HstRequestContext, HstSiteMapItem, boolean)}: that
     * method always first tries to find the canonical location of the jcr Node before it is creating a link for the node. 
     * </p>
     * 
     * <p>
     * <b>Expert:</b> Note there is a difference between context relative with respect to the current URL and with respect to the current jcr Node. <b>Default</b>, links in the HST are
     * created always taking into account the current URL (thus context aware linking) unless you call {@link #createCanonical(Node, HstRequestContext)} or {@link #createCanonical(Node, HstRequestContext, HstSiteMapItem)}. Also,
     * <b>default</b>, it always (unless there is no) takes the <i>canonical</i> location of the jcr Node. Thus, multiple virtual versions of the same physical Node, result in the same HstLink. Only when having <code>navigationStateful = true</code>, 
     * also the jcr Node is context relative, and thus multiple virtual versions of the same jcr Node can result in multiple links. This is interesting for example in 
     * faceted navigation views, where you want 'in context' documents to be shown.
     * </p>
     * @see #create(Node, HstRequestContext, HstSiteMapItem, boolean)
     * @param node the jcr node 
     * @param requestContext the HstRequestContext
     * @param preferredItem  if not null (null means no preferred sitemap item), first a link is trying to be created for this item
     * @param fallback value true or false
     * @param navigationStateful value true or false
     * @return  the HstLink for this jcr Node or <code>null</code>
     */
    HstLink create(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem, boolean fallback, boolean navigationStateful);
    
    /**
     * This creates a canonical HstLink: regardless the current requestContext, one and the same jcr Node is guaranteed to return the same HstLink. This is
     * useful when showing one and the same content via multiple urls, for example in faceted navigation. Search engines can better index your
     * website when defining a canonical location for duplicate contents: See 
     * <a href="http://googlewebmastercentral.blogspot.com/2009/02/specify-your-canonical.html">specify-your-canonical</a> for more info on this subject.
     * 
     * @param node
     * @param requestContext the HstRequestContext
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, HstRequestContext requestContext);
    
   
    /**
     * @see {@link #createCanonical(Node, HstRequestContext)}.
     * When specifying a preferredItem, we try to create a canonical link wrt this preferredItem. If the link cannot be created for this preferredItem,
     * a fallback to {@link #createCanonical(Node, HstRequestContext)} without preferredItem is done.
     * 
     * @param node
     * @param requestContext the HstRequestContext
     * @param preferredItem if <code>null</code>, a fallback to {@link #createCanonical(Node, HstRequestContext)} is done
     * @return the HstLink for this jcr Node or <code>null</code>
     */
    HstLink createCanonical(Node node, HstRequestContext requestContext, HstSiteMapItem preferredItem);
   
    /**
     * Expert: Creates a {@link List} of all available canonical links for <code>node</code> within the hostgroup ( {@link VirtualHost#getHostGroupName()} ) of the {@link Mount} for 
     * the {@link HstRequestContext}. All available links have a {@link Mount} that has at least one of its {@link Mount#getTypes()} equal to the {@link Mount#getTypes()} belonging to the {@link Mount} of the
     * <code>requestContext</code>. If the {@link Mount} of the <code>requestContext</code> has no type in common at all, for example because
     * it is {@link Mount} from a REST mount used by the template composer, you can use {@link #createAllAvailableCanonicals(Node, HstRequestContext, String)} and specify
     * the <code>type</code> the {@link Mount}'s for the available canonical links should be of.
     * @param node
     * @param requestContext the HstRequestContext
     * @return the {@link List} of all available canonical links where at least one of the  {@link Mount#getTypes()} are equal to {@link Mount#getTypes()} belonging to the {@link Mount} of the  <code>requestContext</code> 
     * @see #createCanonical(Node, HstRequestContext)
     */
    List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext);
    
    /**
     * Expert: Creates a {@link List} of all available canonical links for <code>node</code>, within the hostgroup ( {@link VirtualHost#getHostGroupName()} ) of the {@link Mount} for 
     * the {@link HstRequestContext} and where where the backing {@link Mount} of the {@link HstLink} has at least one {@link Mount#getTypes()} equal to <code>type</code>
     * @param node
     * @param requestContext the HstRequestContext
     * @param type the <code>type</code> that the {@link Mount}'s belonging to the available canonical links should be of
     * @return the {@link List} of all available canonical links where at least one of the  {@link Mount#getTypes()} are equal to <code>type</code>
     * @see #createCanonical(Node, HstRequestContext)
     */
    List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext, String type);
    
    /**
     * Expert: Creates a {@link List} of all available canonical links for <code>node</code>, within the hostgroup <code>hostGroupName</code> and where where the backing {@link Mount} of the {@link HstLink} has at least one {@link Mount#getTypes()} equal to <code>type</code>  
     * @param node
     * @param requestContext the HstRequestContext
     * @param type the <code>type</code> that the {@link Mount}'s belonging to the available canonical links should be of
     * @param hostGroupName The hostGroupName that the {@link HstLink}s their {@link Mount}s should belong to
     * @return the {@link List} of all available canonical links where at least one of the  {@link Mount#getTypes()} are equal to <code>type</code>
     * @see #createCanonical(Node, HstRequestContext)
     */
    List<HstLink> createAllAvailableCanonicals(Node node, HstRequestContext requestContext, String type, String hostGroupName);
    
    
    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} with respect to the <code>mount</code>. Note that this HstLink creation does only take into account the
     * <code>mount</code> and not the current context.
     * The <code>mount</code> can be a different one then the one of the current request context.
     * If the <code>mount</code> cannot be used to create a HstLink for the jcr <code>node</code>, because the <code>node</code> belongs
     * to a different (sub)site, a page not found link is returned. </p>
     * <p>note: if a link is returned, this is always the canonical link, also see {@link #createCanonical(Node, HstRequestContext)}</p>
     * @param node the jcr node for that should be translated into a HstLink
     * @param mount the (sub)site for which the hstLink should be created for
     * @return the {@link HstLink} for the jcr <code>node</code> and the <code>mount</code> or <code>null</code> when no link for the node can be made in the <code>mount</code>
     */
    HstLink create(Node node, Mount mount);


    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} for the <code>mountAlias</code>. First, the {@link Mount} belonging to the 
     * <code>mountAlias</code> is searched for. When the {@link Mount} belonging to the alias cannot rewrite the <code>node</code>, 
     * there is no fallback to whether other {@link Mount}'s can rewrite the <code>node</code> to a {@link HstLink}. 
     * 
     * Note that the found {@link Mount} <b>must</b> 
     * 
     * <ol>
     *    <li>have {@link Mount#getAlias()} equal to <code>mountAlias</code></li>
     *    <li>have at least one of its {@link Mount#getTypes()} the same as the {@link Mount} belonging to the current <code>requestContext</code>. 
     *    </li>
     *    <li>have the same {@link VirtualHost#getHostGroupName()} as the {@link Mount} belonging to the current <code>requestContext</code>. 
     *    </li>
     * </ol>
     * 
     * If there is no {@link Mount} complying to the above rules, <code>null</code> is returned. <b>If</b> a {@link Mount} does comply, we return  {@link #create(Node, Mount)} 
     * </p>
     * @param node the jcr node
     * @param requestContext the current request context
     * @param mountAlias the alias of the {@link Mount} for which the link should be created for
     * @return the {@link HstLink} for the jcr <code>node</code> and the <code>mountAlias</code> or <code>null</code>  when there cannot be found an {@link Mount} for the alias
     * @see {@link #create(Node, Mount)} 
     */
    HstLink create(Node node, HstRequestContext requestContext, String mountAlias);
    
    /**
     * <p>Expert: Rewrite a jcr <code>node</code> to a {@link HstLink} for the <code>mountAlias</code> and for <code>type</code>. When the {@link Mount} 
     * belonging to the alias cannot rewrite the <code>node</code>, there is no fallback to whether other {@link Mount}'s can rewrite the <code>node</code> to a {@link HstLink}
     * 
     * Note that the found {@link Mount} <b>must</b> 
     * 
     * <ol>
     *    <li>have {@link Mount#getAlias()} equal to <code>mountAlias</code></li>
     *    <li>contain <code>type</code> in its {@link Mount#getTypes()}
     *    <li>have the same {@link VirtualHost#getHostGroupName()} as the {@link Mount} belonging to the current <code>requestContext</code>. 
     *    </li>
     * </ol>
     * 
     * If there is no {@link Mount} complying to the above rules, <code>null</code> is returned. <b>If</b> a {@link Mount} does comply, we return  {@link #create(Node, Mount)}.
     * 
     * </p>
     * <p>
     * The difference with {@link #create(Node, HstRequestContext, String))} is that this method does not look for a {@link Mount} with a common <code>type</code> as for the {@link Mount} from the current request. It does
     * look for a {@link Mount} which at least has <code>type</code> as its {@link Mount#getTypes()} 
     * </p>
     * @param node the jcr node
     * @param requestContext the current request context
     * @param mountAlias the alias of the {@link Mount} for which the link should be created for
     * @param type the type tha should be contained in the {@link Mount#getTypes()} where the {@link Mount} is the mount belonging to the returned {@link HstLink}
     * @return the {@link HstLink} for the jcr <code>node</code> and the <code>mountAlias</code> or <code>null</code> when no link for the node can be made in the <code>{@link Mount}</code> belonging to the alias or when there belongs no {@link Mount} to the alias
     * @see {@link #create(Node, Mount)} 
     */
    HstLink create(Node node, HstRequestContext requestContext, String mountAlias, String type);
    
    /**
     * 
     * @param bean
     * @param requestContext the HstRequestContext
     * @return a HstLink for <code>bean</code> and the <code>hstRequestContext</code> or <code>null</code> when no link for the node can be made
     */
    HstLink create(HippoBean bean, HstRequestContext requestContext);

    /**
     * Regardless the current context, create a HstLink to the HstSiteMapItem that you use as argument. This is only possible if the sitemap item does not
     * contain any ancestor including itself with a wildcard, because the link is ambiguous in that case. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param toHstSiteMapItem the {@link HstSiteMapItem} to link to
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(HstSiteMapItem toHstSiteMapItem, Mount mount);
    
    /**
     * Regardless the current context, create a HstLink to the {@link HstSiteMapItem} for {@link Mount} <code>mount</code>  that has {@link HstSiteMapItem#getRefId()} equal to <code>siteMapItemRefId</code>. 
     * If there cannot be found a {@link HstSiteMapItem} for <code>siteMapItemRefId</code> in the {@link Mount} <code>mount</code>, then <code>null</code> is returned.
     * If the {@link HstSiteMapItem} is found for <code>siteMapItemRefId</code>, then it can be only used when it does not
     * contain any ancestor including itself with a wildcard, because the link is ambiguous in that case. 
     * If a wildcard is encountered, this method can return <code>null</code>, though this is up to the implementation
     * @param siteMapItemRefId the {@link HstSiteMapItem#getRefId()} of the {@link HstSiteMapItem} to link to
     * @param mount the {@link Mount} the <code>siteMapItemRefId</code> should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink createByRefId(String siteMapItemRefId, Mount mount);

    /**
     * Regardless the current context, create a HstLink for the <code>path</code> and <code>mount</code>
     * @param path the path to the sitemap item
     * @param mount the {@link Mount} the path should be in
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, Mount mount);
     
    /**
     * Regardless the current context, create a HstLink to the path that you use as argument. 
     * @param path the path to the sitemap item
     * @param mount the {@link Mount} for which the link should be created
     * @param containerResource whether it is a static link, for example for css/js
     * @return an <code>HstLink</code> instance or <code>null<code> 
     */
    HstLink create(String path, Mount mount, boolean containerResource);

    /**
     * @return a link that can be used for page not found links for <code>mount</code>
     */
    HstLink createPageNotFoundLink(Mount mount);

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

    /**
     * Clears possibly available caches
     */
    void clear();

}
