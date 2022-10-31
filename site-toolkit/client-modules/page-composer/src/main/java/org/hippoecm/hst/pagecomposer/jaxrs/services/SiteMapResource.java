/*
 * Copyright 2014-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyEventImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCreateContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCreateEventImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageDeleteContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageDeleteEventImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageMoveContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageMoveEventImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageUpdateContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageUpdateEventImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.MountRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapTreeItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.SiteMapTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.NotNullValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorBuilder;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapRepresentationService;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapShallowItem;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapTreeItem;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapTreeItemUtils;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.DocumentUtils.findAvailableDocumentRepresentations;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.DocumentUtils.getDocumentRepresentationHstConfigUser;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMAP + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMapResource extends AbstractConfigResource {

    private static final Logger log = LoggerFactory.getLogger(SiteMapResource.class);

    private SiteMapHelper siteMapHelper;
    private ValidatorFactory validatorFactory;
    private boolean hideXPages;
    private XPageSiteMapRepresentationService xPageSiteMapRepresentationService;

    public void setSiteMapHelper(final SiteMapHelper siteMapHelper) {
        this.siteMapHelper = siteMapHelper;
    }

    public void setValidatorFactory(final ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    public void setHideXPages(final boolean hideXPages) {
        this.hideXPages = hideXPages;
    }

    public void setxPageSiteMapRepresentationService(final XPageSiteMapRepresentationService xPageSiteMapRepresentationService) {
        this.xPageSiteMapRepresentationService = xPageSiteMapRepresentationService;
    }

    @GET
    @Path("/mount")
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getMountRepresentation() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Mount mount = getPageComposerContextService().getEditingMount();
                return ok("Hostname loaded successfully", new MountRepresentation().represent(mount));
            }
        });
    }

    @GET
    @Path("/sitemapitem")
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getSiteMapShallowItem(final @Context HttpServletRequest request) {
        return getSiteMapShallowItem(request, StringUtils.EMPTY);
    }

    @GET
    @Path("/sitemapitem/{pathInfo: .*}")
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getSiteMapShallowItem(final @Context HttpServletRequest request,
                                          final @PathParam("pathInfo") String pathInfo) {
        return tryGet(() -> {
            final Mount editingPreviewMount = getPageComposerContextService().getEditingMount();

            final String siteMapIdentifier = getPageComposerContextService().getRequestConfigIdentifier();

            final HstSiteMap siteMap = editingPreviewMount.getHstSite().getSiteMap();

            if (!(siteMap instanceof CanonicalInfo)) {
                throw new IllegalStateException(format("Expected a siteMap instance of type %s", CanonicalInfo.class.getName()));
            }

            if (!siteMapIdentifier.equals(((CanonicalInfo)siteMap).getCanonicalIdentifier())) {
                throw new ClientException(format("Sitemap identifier '%s' does not belong to the currently edited channel '%s'",
                        siteMapIdentifier, editingPreviewMount.getChannel().getName()), ClientError.UNKNOWN);
            }

            final XPageSiteMapTreeItem xPageSiteMapTreeItem =
                    xPageSiteMapRepresentationService.getSiteMapTree(editingPreviewMount, HstConfigurationUtils.getPreviewHstModel());

            final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(request.getSession());

            final SessionSecurityDelegation sessionSecurityDelegation = HstServices.getComponentManager().getComponent(SessionSecurityDelegation.class.getName());

            final Session previewSecurityDelegate = sessionSecurityDelegation.createPreviewSecurityDelegate(cmsSessionContext.getRepositoryCredentials(), true);

            final Optional<XPageSiteMapShallowItem> readableItem = XPageSiteMapTreeItemUtils.getReadableItem(xPageSiteMapTreeItem, previewSecurityDelegate, pathInfo);

            SiteMapTreeItem pathInfoItem = null;
            if (readableItem.isPresent()) {
                final XPageSiteMapShallowItem pathInfoRoot = readableItem.get();
                final Optional<SiteMapPageRepresentation> pathInfoRootRepresentation = getSiteMapPageRepresentation(previewSecurityDelegate, pathInfoRoot);

                if (pathInfoRoot.isExpandable()) {
                    final XPageSiteMapShallowItem[] readableChildren = XPageSiteMapTreeItemUtils.getReadableChildren(xPageSiteMapTreeItem, previewSecurityDelegate, pathInfo);
                    pathInfoItem = new SiteMapTreeItem(pathInfoRootRepresentation.get());

                    for (XPageSiteMapShallowItem item : readableChildren) {

                        try {
                            // read access guaranteed because of XPageSiteMapTreeItemUtils.getReadableChildren
                            final Optional<SiteMapPageRepresentation> siteMapPageRepresentation = getSiteMapPageRepresentation(previewSecurityDelegate, item);
                            if (siteMapPageRepresentation.isPresent()) {
                                pathInfoItem.addChild(new SiteMapTreeItem(siteMapPageRepresentation.get()));
                            }
                        } catch (Exception e) {
                            log.warn("Exception while trying to add '{}'. Skipping this sitemap item. Reason : {}", item.getPathInfo(), e.getMessage());
                        }
                    }
                }
            }


            // MERGE HST sitemap routes
            // try to get it from the HST routes (sitemap)
            final Mount editingMount = getPageComposerContextService().getEditingMount();

            final SiteMapPagesRepresentation pages = new SiteMapPagesRepresentation().represent(siteMap,
                    editingMount, getPreviewConfigurationPath());

            final SiteMapTreeItem routes = SiteMapTreeItem.transform(pages);

            // no XPage document matched for pathInfo, check sitemap
            if (pathInfoItem == null) {

                if (StringUtils.isEmpty(pathInfo)) {
                    // the root item
                    pathInfoItem = routes.shallowClone();
                } else {
                    final Optional<SiteMapTreeItem> routesItem = getShallowSiteMapTreeItem(pathInfo, routes);
                    if (!routesItem.isPresent()) {
                        throw new ClientException(format("Cannot find a sitemap item or XPage document for '%s'", pathInfo), ClientError.ITEM_NOT_FOUND);
                    }
                    pathInfoItem = routesItem.get();
                }

            } else {
                final Optional<SiteMapTreeItem> routesItem = getShallowSiteMapTreeItem(pathInfo, routes);
                if (routesItem.isPresent()) {
                    // merge sitemap routes with XPage documents if needed
                    final SiteMapTreeItem xpagesBasedItem = pathInfoItem;
                    routesItem.get().getChildren().stream().forEach(
                            routeItem -> {
                                if (xpagesBasedItem.getChild(routeItem.getId()) == null) {
                                    xpagesBasedItem.addChild(routeItem);
                                    // in case the xpagesBasedItem was not yet expandable, mark it to be so now
                                    xpagesBasedItem.setExpandable(true);
                                }
                            }
                    );
                }
            }

            return ok(format("Item for pathInfo '%s'", pathInfo), pathInfoItem);
        });
    }

    /**
     * <p>
     *     Returns shallow sitemap tree item in which children from the children for SiteMapTreeItem belonging to
     *     {@code pathInfo} are removed.
     * </p>
     */
    private Optional<SiteMapTreeItem> getShallowSiteMapTreeItem(final String pathInfo, final SiteMapTreeItem routes) {
        SiteMapTreeItem pathInfoItem = routes;
        if (StringUtils.isNotEmpty(pathInfo) && !pathInfo.equals("/")) {
            final String[] elements = pathInfo.split("/");
            for (String element : elements) {
                pathInfoItem = pathInfoItem.getChild(element);
                if (pathInfoItem == null) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(pathInfoItem.shallowClone());
    }

    private Optional<SiteMapPageRepresentation> getSiteMapPageRepresentation(final Session previewSecurityDelegate, final XPageSiteMapShallowItem item) throws RepositoryException {
        final String absoluteJcrPath = item.getAbsoluteJcrPath();
        if (absoluteJcrPath == null) {
            // structural sitemap item, not backed by an actual XPage document but will be expandable

            return Optional.of(SiteMapPageRepresentation.structural(item.getPathInfo(), item.isExpandable()));
        } else {
            final Node handle = previewSecurityDelegate.getNode(absoluteJcrPath);
            return getSiteMapPageRepresentation(getPageComposerContextService().getRequestContext(), handle, item.isExpandable());
        }
    }

    // below CHANNEL_VIEWER_PRIVILEGE_NAME privilege since web authors not being allowed to modify hst config still
    // need to be able to load the available sitemap pages for navigation
    @GET
    @Path("/pages")
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getSiteMapPages(@Context HttpServletRequest servletRequest) {
        return tryGet(() -> {
            final SiteMapPagesRepresentation pages = getPages(servletRequest);
            return ok("Sitemap loaded successfully", pages);
        });
    }

    @GET
    @Path("/pagetree")
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getPageTree(@Context HttpServletRequest servletRequest) {
        return tryGet(() -> {
            final SiteMapPagesRepresentation pages = getPages(servletRequest);

            return ok("Page tree loaded successfully", SiteMapTreeItem.transform(pages));
        });
    }

    private SiteMapPagesRepresentation getPages(final HttpServletRequest servletRequest) {
        final HstSite site = getPageComposerContextService().getEditingPreviewSite();
        final HstSiteMap siteMap = site.getSiteMap();
        final Mount editingMount = getPageComposerContextService().getEditingMount();
        final SiteMapPagesRepresentation pages = new SiteMapPagesRepresentation().represent(siteMap,
                editingMount, getPreviewConfigurationPath());

        if (hideXPages) {
            return pages;
        }

        if (servletRequest == null) {
            // unit testing
            return pages;
        }

        // append the XPages for the current channel which are readable for the current CMS user. Note, no paging, just
        // all the available XPages. Note that adding them is not allowed in SiteMapPagesRepresentation.represent()
        // since that presentation is also used for creating a *new* hst configuration page (sitemap item)

        // we need a preview security delegate to make sure that we get to see the PREVIEW XPages which are
        // readable by the current cms user *or* by the HST preview user
        try {
            final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(servletRequest.getSession());

            final SessionSecurityDelegation sessionSecurityDelegation = HstServices.getComponentManager().getComponent(SessionSecurityDelegation.class.getName());

            Session previewSecurityDelegate = sessionSecurityDelegation.createPreviewSecurityDelegate(cmsSessionContext.getRepositoryCredentials(), true);

            List<SiteMapPageRepresentation> xpages = getXPageRepresentations(editingMount, previewSecurityDelegate,
                    getPageComposerContextService().getRequestContext());


            // from xpages, filter out the XPages that are already represented by an explicit sitemap item which
            // can happen : We can filter them out on the 'renderPathInfo' since there is no point in including
            // to sitemap pages with the exact same link (and thus a duplicate)

            final List<SiteMapPageRepresentation> filteredDuplicates = xpages.stream()
                    .filter(xpage ->
                            // only if there is NO hst config sitemap page representation for the XPage include the xpage
                            !pages.getPages().stream()
                                    .anyMatch(page ->
                                    {
                                        if (page.getRenderPathInfo() == null) {
                                            return false;
                                        }
                                        return page.getRenderPathInfo().equals(xpage.getRenderPathInfo());
                                    })
                    )
                    .collect(Collectors.toList());

            pages.getPages().addAll(filteredDuplicates);

            // sort now the XPage have been added again on pathInfo
            Collections.sort(pages.getPages(), Comparator.comparing(SiteMapPageRepresentation::getPathInfo));

        } catch (Exception e) {
            log.warn("Exception occurred while trying to load XPage Documents for the sitemap. Only the SiteMap " +
                    "Pages are returned" , e);
        }
        return pages;
    }

    private List<SiteMapPageRepresentation> getXPageRepresentations(final Mount editingMount, final Session session,
                                                                    final HstRequestContext requestContext) throws RepositoryException {

        List<SiteMapPageRepresentation> xpages = new ArrayList();

        final Node contentRoot = session.getNode(editingMount.getContentPath());

        // do not use the content root path as scope since this results in *slow* queries *and* if the
        // content root node name starts with a number, you need to escape it, which is unhandy
        final String statement = format("//element(*,%s)[@hippo:paths = '%s' and @hippo:availability = 'preview']",
                MIXINTYPE_HST_XPAGE_MIXIN, contentRoot.getIdentifier());

        final Query xPagesQuery = session.getWorkspace().getQueryManager().createQuery(statement, "xpath");

        for (Node unpublishedVariant : new NodeIterable(xPagesQuery.execute().getNodes())) {

            // we do not know whether expandable or not, but we just mark it as expandable: the FE does not yet use it
            getSiteMapPageRepresentation(requestContext, unpublishedVariant.getParent(), true).ifPresent(
                    page -> xpages.add(page)
            );

        }

        return xpages;
    }

    private Optional<SiteMapPageRepresentation> getSiteMapPageRepresentation(final HstRequestContext requestContext, final Node handle,
                                                                             final boolean expandable)
            throws RepositoryException {

        if (!handle.isNodeType(NT_HANDLE)) {
            log.info("Skipping unexpected node '{}' with mixin '{}' : only document are expected",
                    MIXINTYPE_HST_XPAGE_MIXIN, handle.getPath());
            return Optional.empty();
        }

        // use the link creator to get hold of URLs for the XPages
        // Note this is very delicate: we only try to resolve the XPages *WITHIN* the current channel (editingMount),
        // hence we use create(Node node, Mount editingMount) : if no link can be found, we just skip (for now?)
        // the XPage!
        final Mount editingMount = getPageComposerContextService().getEditingMount();
        final HstLink hstLink = requestContext.getHstLinkCreator().create(handle, editingMount);
        if (hstLink.isNotFound()) {
            log.info("Skipping XPage '{}' since cannot be represented in channel '{}'", handle.getPath(),
                    editingMount.getChannel().getName());
            return Optional.empty();
        }

        final SiteMapPageRepresentation represent = new SiteMapPageRepresentation().represent(hstLink, handle);
        represent.setExpandable(expandable);
        return Optional.of(represent);
    }

    @GET
    @Path("/item/{siteMapItemUuid}")
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getSiteMapItem(@PathParam("siteMapItemUuid") final String siteMapItemUuid) {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMapItem siteMapItem = siteMapHelper.getConfigObject(siteMapItemUuid);
                final HstSite site = getPageComposerContextService().getEditingPreviewSite();
                final HstComponentsConfiguration componentsConfiguration = site.getComponentsConfiguration();
                String componentConfigurationId = siteMapItem.getComponentConfigurationId();
                final HstComponentConfiguration page = componentsConfiguration.getComponentConfiguration(componentConfigurationId);

                DocumentRepresentation primaryDocumentRepresentation = null;
                final String relativeContentPath = siteMapItem.getRelativeContentPath();
                final String rootContentPath = getPageComposerContextService().getEditingMount().getContentPath();
                if (relativeContentPath != null) {
                    final String absPath;
                    if (relativeContentPath.startsWith("/")) {
                        absPath = rootContentPath + relativeContentPath;
                    } else {
                        absPath = rootContentPath + "/" + relativeContentPath;
                    }
                    primaryDocumentRepresentation = getDocumentRepresentationHstConfigUser(absPath);
                    primaryDocumentRepresentation.setSelected(true);
                }
                Set<DocumentRepresentation> availableDocumentRepresentations = findAvailableDocumentRepresentations(
                        getPageComposerContextService(), page, primaryDocumentRepresentation, true, rootContentPath + "/");

                final SiteMapItemRepresentation siteMapItemRepresentation = new SiteMapItemRepresentation()
                        .represent(siteMapItem, getPageComposerContextService().getEditingMount(), primaryDocumentRepresentation, availableDocumentRepresentations);

                return ok("Sitemap item loaded successfully", siteMapItemRepresentation);
            }
        });
    }

    @GET
    @Path("/picker")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response getSiteMapTreePicker() {
        return tryGet(() -> {
            final AbstractTreePickerRepresentation representation = SiteMapTreePickerRepresentation.representRequestSiteMap(getPageComposerContextService());
            return ok("Sitemap loaded successfully", representation);
        });
    }

    /**
     * @param siteMapItemRefIdOrPath
     * @return the rest response to create the client tree for <code>siteMapPathInfo</code> : the response contains the
     * sitemap tree with all ancestor <code>HstSiteMapItem</code>'s of parameter <code>siteMapitemRefIdorPath</code>
     * expanded.
     */
    @GET
    @Path("/picker/{siteMapItemRefIdOrPath: .*}")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response getSiteMapTreePicker(final @PathParam("siteMapItemRefIdOrPath") String siteMapItemRefIdOrPath) {
        return tryGet(() -> {
            final PageComposerContextService service = getPageComposerContextService();
            final HstSiteMapItem item = getHstSiteMapItem(siteMapItemRefIdOrPath, service);
            final AbstractTreePickerRepresentation representation = item != null
                    ? SiteMapTreePickerRepresentation.representExpandedParentTree(service, item)
                    : SiteMapTreePickerRepresentation.representRequestSiteMap(service);

            return ok("Sitemap for item " + siteMapItemRefIdOrPath + " loaded successfully", representation);
        });
    }

    private static HstSiteMapItem getHstSiteMapItem(final String siteMapItemRefIdOrPath, final PageComposerContextService service) {
        if (StringUtils.isEmpty(siteMapItemRefIdOrPath)) {
            return null;
        }

        final HstSite site = service.getEditingPreviewSite();
        final HstSiteMap siteMap = site.getSiteMap();
        HstSiteMapItem item = siteMap.getSiteMapItem(siteMapItemRefIdOrPath);
        if (item == null) {
            item = siteMap.getSiteMapItemById(siteMapItemRefIdOrPath);
        }

        if (item == null){
            item = siteMap.getSiteMapItemByRefId(siteMapItemRefIdOrPath);
        }
        return item;
    }

    @POST
    @Path("/update")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response update(final SiteMapItemRepresentation siteMapItem) {
        final ValidatorBuilder preValidatorBuilder = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getConfigurationExistsValidator(siteMapItem.getId(), siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        siteMapItem.getId(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getNameValidator(siteMapItem.getName()))
                .add(validatorFactory.getPathInfoValidator(getPageComposerContextService(), siteMapItem, null, siteMapHelper))
                .add(new NotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME));
        if (siteMapItem.getParentId() != null) {
            preValidatorBuilder.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    siteMapItem.getParentId(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                    .add(validatorFactory.getConfigurationExistsValidator(siteMapItem.getParentId(), siteMapHelper));

            // if parent item is different than current parent, then the parent item is not allowed to be same as or a descendant of siteMapItem
            final HstSiteMapItem existingParent = siteMapHelper.getConfigObject(siteMapItem.getId()).getParentItem();
            if (existingParent == null || ((existingParent instanceof CanonicalInfo)
                    && !((CanonicalInfo)existingParent).getCanonicalIdentifier().equals(siteMapItem.getParentId()))) {
                // update also involves a move!
                preValidatorBuilder.add(validatorFactory.getItemNotSameOrDescendantOfValidator(siteMapItem.getParentId(), siteMapItem.getId()));
            }
        }


        // if the update has a uuid for component id, we need to re-apply a prototype. In that case we also need to
        // validate the prototype page
        boolean isCompIdUUID = false;
        if (siteMapItem.getComponentConfigurationId() != null) {
            try {
                UUID.fromString(siteMapItem.getComponentConfigurationId());
                // new page id: reapply prototype
                preValidatorBuilder.add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()));
                isCompIdUUID = true;
            } catch (IllegalArgumentException e) {
                // no problem: no new page id has been set
            }
        }

        final boolean reapplyPrototype = isCompIdUUID;
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final PageUpdateContext pageUpdateContext = siteMapHelper.update(siteMapItem, reapplyPrototype);
                publishSynchronousEvent(new PageUpdateEventImpl(getPageComposerContextService().getEditingPreviewChannel(), pageUpdateContext));
                log.debug("Published page update event with context: {}", pageUpdateContext);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(siteMapItem.getId(), null);
                return ok("Item updated successfully", siteMapPageRepresentation);
            }
        }, preValidatorBuilder.build());
    }

    @POST
    @Path("/create")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response create(final SiteMapItemRepresentation siteMapItem) {
        return create(siteMapItem, null);
    }

    @POST
    @Path("/create/{parentId}")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response create(final SiteMapItemRepresentation siteMapItem,
                           final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), true, getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()))
                .add(validatorFactory.getNameValidator(siteMapItem.getName()))
                .add(validatorFactory.getPathInfoValidator(getPageComposerContextService(), siteMapItem, parentId, siteMapHelper));

        if (parentId != null) {
            preValidators.add(validatorFactory.getConfigurationExistsValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final PageCreateContext pageCreateContext = siteMapHelper.create(siteMapItem, parentId);
                publishSynchronousEvent(new PageCreateEventImpl(getPageComposerContextService().getEditingPreviewChannel(), pageCreateContext));
                log.debug("Published page create event with context: {}", pageCreateContext);
                SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(pageCreateContext.getNewSiteMapItemNode().getIdentifier(), parentId);
                return ok("Item created successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }

    @POST
    @Path("/copy")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response copy(
            @HeaderParam("mountId")final String mountId,
            @HeaderParam("siteMapItemUUID") final String siteMapItemUUID,
            @HeaderParam("targetSiteMapItemUUID")final String targetSiteMapItemUUID,
            @HeaderParam("targetName")final String targetName
    ) {

        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getNotNullValidator(targetName,
                        ClientError.INVALID_NAME, "Name of the copied page is not allowed to be null"))
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getNameValidator(targetName))
                .add(validatorFactory.getNamePathInfoValidator(getPageComposerContextService(), targetName))
                .add(validatorFactory.getConfigurationExistsValidator(siteMapItemUUID, siteMapHelper));


        if (StringUtils.isNotBlank(targetSiteMapItemUUID)) {
            if (StringUtils.isBlank(mountId) || getPageComposerContextService().getEditingMount().getIdentifier().equals(mountId)) {
                preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                                         targetSiteMapItemUUID, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                             .add(validatorFactory.getConfigurationExistsValidator(targetSiteMapItemUUID, siteMapHelper));
            } else {
                // validate targetSiteMapItemUUID is located in preview workspace sitemap
                preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(mountId),
                                        targetSiteMapItemUUID, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                           // validate that the 'targetSiteMapItemUUID' is currently part of the hst model
                           .add(validatorFactory.getConfigurationExistsValidator(targetSiteMapItemUUID, mountId, siteMapHelper))
                           .add(validatorFactory.getTargetMountIsPartOfEditingMountHstModel(getMount(mountId), getPageComposerContextService().getEditingMount()));
            }
        }

        if (StringUtils.isNotBlank(mountId)) {
            // validate target mount has preview and a workspace
            preValidators.add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService(), mountId));
            preValidators.add(validatorFactory.getHasWorkspaceConfigurationValidator(getPageComposerContextService(), mountId));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final PageCopyContext pageCopyContext = siteMapHelper.copy(mountId, siteMapItemUUID,
                        targetSiteMapItemUUID, targetName);
                PageCopyEventImpl event = new PageCopyEventImpl(getPageComposerContextService().getEditingPreviewChannel(), pageCopyContext);
                publishSynchronousEvent(event);
                log.debug("Published page copy event with context: {}", pageCopyContext);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(pageCopyContext.getTargetMount(),
                        pageCopyContext.getNewSiteMapItemNode().getIdentifier(), null);
                return ok("Item created successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }


    /**
     * if <code>parentId</code> is <code>null</code> the move will be done to the root sitemap
     */
    @POST
    @Path("/move/{id}/{parentId}")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response move(final @PathParam("id") String id,
                         final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getConfigurationExistsValidator(id, siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));

        if (parentId != null) {
            preValidators.add(validatorFactory.getConfigurationExistsValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final PageMoveContext pageMoveContext = siteMapHelper.move(id, parentId);
                publishSynchronousEvent(new PageMoveEventImpl(getPageComposerContextService().getEditingPreviewChannel(), pageMoveContext));
                log.debug("Published page move event with context: {}", pageMoveContext);
                final SiteMapPageRepresentation siteMapPageRepresentation = createSiteMapPageRepresentation(id, parentId);
                return ok("Item moved successfully", siteMapPageRepresentation);
            }
        }, preValidators.build());
    }

    @POST
    @Path("/delete/{id}")
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response delete(final @PathParam("id") String id) {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getConfigurationExistsValidator(id, siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .build();

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final PageDeleteContext pageDeleteContext = siteMapHelper.delete(id);
                publishSynchronousEvent(new PageDeleteEventImpl(getPageComposerContextService().getEditingPreviewChannel(), pageDeleteContext));
                log.debug("Published page delete event with context: {}", pageDeleteContext);
                return ok("Item deleted successfully", id);
            }
        }, preValidator);
    }

    private SiteMapPageRepresentation createSiteMapPageRepresentation(final String siteMapItemUUID, final String parentId) throws RepositoryException {
        return createSiteMapPageRepresentation(getPageComposerContextService().getEditingMount(), siteMapItemUUID, parentId);
    }

    private SiteMapPageRepresentation createSiteMapPageRepresentation(final Mount target, final String siteMapItemUUID, final String parentId) throws RepositoryException {
        SiteMapPageRepresentation siteMapPageRepresentation = new SiteMapPageRepresentation();
        siteMapPageRepresentation.setId(siteMapItemUUID);
        // siteMapPathInfo without starting /
        Node siteMapItem = getPageComposerContextService().getRequestContext().getSession().getNodeByIdentifier(siteMapItemUUID);
        String siteMapPathInfo = StringUtils.substringAfter(siteMapItem.getPath(), NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_SITEMAP );
        siteMapPageRepresentation.setPathInfo(siteMapPathInfo.substring(1));
        siteMapPageRepresentation.setRenderPathInfo(target.getMountPath() + siteMapPathInfo);
        siteMapPageRepresentation.setParentId(parentId);
        return siteMapPageRepresentation;
    }

}
