/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.onehippo.cms7.essentials.shared.model.Dependency;
import org.onehippo.cms7.essentials.shared.model.Plugin;
import org.onehippo.cms7.essentials.shared.model.PluginCollection;
import org.onehippo.cms7.essentials.shared.model.Vendor;
import org.onehippo.cms7.essentials.shared.model.Version;
import org.onehippo.cms7.essentials.site.beans.PluginDocument;
import org.onehippo.cms7.essentials.site.beans.VendorDocument;
import org.onehippo.cms7.essentials.site.beans.VersionDocument;
import org.onehippo.cms7.essentials.site.beans.compounds.DependencyCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: PluginResource.java 157508 2013-03-10 13:02:32Z fvlankvelt $"
 */
@Path("/")
@Produces({MediaType.APPLICATION_JSON})
public class PluginResource extends AbstractResource {

    private static Logger log = LoggerFactory.getLogger(PluginResource.class);

    @GET
    @Path("/plugins/")
    public PluginCollection getPluginDescriptors(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context UriInfo info) {
        final HstRequestContext context = getRequestContext(request);
        try {
            final HippoBean bean = getMountContentBaseBean(context);
            @SuppressWarnings("unchecked")
            final HstQuery query = getHstQueryManager(context).createQuery(bean, PluginDocument.class);
            final HstQueryResult execute = query.execute();
            final HippoBeanIterator hippoBeans = execute.getHippoBeans();
            PluginCollection collection = new PluginCollection();
            while (hippoBeans.hasNext()) {

                final HippoBean hippoBean = hippoBeans.nextHippoBean();
                if (hippoBean != null) {
                    PluginDocument document = (PluginDocument) hippoBean;
                    Plugin plugin = new Plugin();
                    populateVendor(context, document, plugin);
                    final String id = document.getPluginId();
                    plugin.setId(id);
                    plugin.setName(document.getName());
                    plugin.setDescription(document.getDescription());
                    List<VersionDocument> versions = getVersions(context, id);
                    for (VersionDocument version : versions) {
                        plugin.addVersion(parseVersion(version));
                    }
                    collection.addPlugin(plugin);
                }
            }
            collection.setDescription("Hippo plugin repository");
            return collection;
        } catch (QueryException e) {
            log.error("Error creating query", e);
        } catch (ObjectBeanManagerException e) {
            log.error("Error fetching  bean", e);
        }
        return new PluginCollection();
    }

    @GET
    @Path("/plugins/{id}")
    public Plugin getPlugin(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context UriInfo info,
                            @PathParam("id") String id) {

        final HstRequestContext context = getRequestContext(request);
        try {
            final HippoBean bean = getMountContentBaseBean(context);
            @SuppressWarnings("unchecked")
            final HstQuery query = getHstQueryManager(context).createQuery(bean, PluginDocument.class);
            Filter filter = query.createFilter();
            query.setLimit(1);
            filter.addEqualTo("hippoplugins:pluginid", id);
            query.setFilter(filter);
            final HstQueryResult execute = query.execute();
            final HippoBeanIterator hippoBeans = execute.getHippoBeans();

            if (hippoBeans.hasNext()) {

                final HippoBean hippoBean = hippoBeans.nextHippoBean();
                if (hippoBean != null) {
                    PluginDocument document = (PluginDocument) hippoBean;
                    Plugin plugin = new Plugin();
                    plugin.setId(document.getPluginId());
                    populateVendor(context, document, plugin);
                    plugin.setDescription(document.getDescription());
                    List<VersionDocument> versions = getVersions(context, id);
                    for (VersionDocument version : versions) {
                        plugin.addVersion(parseVersion(version));
                    }
                    return plugin;
                }
            }
        } catch (QueryException e) {
            log.error("Error creating query", e);
        } catch (ObjectBeanManagerException e) {
            log.error("Error fetching  bean", e);
        }
        return new Plugin();
    }

    @Override
    public ContentRewriter<String> getContentRewriter() {
        return new SimpleContentRewriter();
    }

    public List<VersionDocument> getVersions(final HstRequestContext context, final String id) throws ObjectBeanManagerException, QueryException {
        final HippoBean bean = getMountContentBaseBean(context);
        @SuppressWarnings("unchecked")
        final HstQuery query = getHstQueryManager(context).createQuery(bean, VersionDocument.class);
        Filter filter = query.createFilter();
        filter.addEqualTo("hippoplugins:pluginid", id);
        query.setFilter(filter);
        final HstQueryResult execute = query.execute();
        final HippoBeanIterator hippoBeans = execute.getHippoBeans();
        final List<VersionDocument> versionDocuments = new ArrayList<VersionDocument>();
        while (hippoBeans.hasNext()) {
            VersionDocument document = (VersionDocument) hippoBeans.nextHippoBean();
            versionDocuments.add(document);
        }
        return versionDocuments;
    }

    private Version parseVersion(final VersionDocument version) {
        final Version v = new Version();
        v.setVersion(version.getVersion());
        final List<DependencyCompound> dependencies = version.getDependencies();
        addDependencies(v, dependencies);
        return v;
    }

    private void populateVendor(final HstRequestContext context, final PluginDocument document, final Plugin plugin) {
        final VendorDocument vendor = document.getVendor();
        if (vendor != null) {
            final Vendor v = new Vendor();
            v.setName(vendor.getName());
            final String logo = createLogoUrl(context, vendor);
            v.setLogo(logo);
            v.setName(vendor.getName());
            plugin.setVendor(v);
        }
    }

    private String createLogoUrl(final HstRequestContext context, final VendorDocument vendor) {
        final HippoGalleryImageSetBean logo = vendor.getLogo();
        if (logo == null) {
            return null;
        }
        final HstLinkCreator creator = context.getHstLinkCreator();
        final HstLink hstLink = creator.create(logo, context);
        if (hstLink == null) {
            return null;
        }
        return hstLink.toUrlForm(context, true);

    }

    private void addDependencies(final Version version, final List<DependencyCompound> dependencies) {
        if (dependencies != null) {
            for (DependencyCompound dependency : dependencies) {
                final Dependency d = new Dependency();
                d.setProjectType(dependency.getProjectType());
                d.setArtifactId(dependency.getArtifactId());
                d.setGroupId(dependency.getGroupId());
                d.setScope(dependency.getScope());
                d.setVersion(dependency.getVersion());
                version.addDependency(d);
            }
        }
    }
}
