package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.CatalogObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * @version "$Id: ComponentsUtils.java 174059 2013-08-16 13:51:28Z mmilicevic $"
 */
public final class ComponentsUtils {

    public static final String CATALOG_PATH = "hst:catalog";
    public static final String HST_CONFIG_PATH = "hst:hst/hst:configurations";
    public static final String HIPPOESSENTIALS_CATALOG = "hippoessentials-catalog";
    private static Logger log = LoggerFactory.getLogger(ComponentsUtils.class);

    private ComponentsUtils() {

    }

    private static final Set<String> HIPPO_BUILT_IN_SITES = new ImmutableSet.Builder<String>()
            .add("hst:default")
            .build();
    private static final Predicate<String> BUILTIN_SITES_PREDICATE = new Predicate<String>() {
        @Override
        public boolean apply(String namespace) {
            return HIPPO_BUILT_IN_SITES.contains(namespace);
        }
    };


    public static List<String> getCustomSites(final PluginContext context) {
        final List<String> allAvailableSites = getAllAvailableSites(context);
        return  ImmutableList.copyOf(Iterables.filter(allAvailableSites, Predicates.not(BUILTIN_SITES_PREDICATE)));
    }


    public static List<String> getAllAvailableSites(final PluginContext context){
        final List<String> sites = new ArrayList<>();
        try {
            final Session session = context.getSession();
            final Node rootNode = session.getRootNode();
            final Node node = rootNode.getNode(HST_CONFIG_PATH);
            final NodeIterator nodes = node.getNodes();
            while(nodes.hasNext()){
                sites.add(nodes.nextNode().getName());
            }

        } catch (RepositoryException e) {
            log.error("Error retrieving HST config", e);
        }

        return sites;

    }


    /*
    *
    *     <sv:node sv:name="list">
      <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>hst:containeritemcomponent</sv:value>
      </sv:property>
      <sv:property sv:name="hst:componentclassname" sv:type="String">
        <sv:value>org.onehippo.cms7.essentials.site.components.ListView</sv:value>
      </sv:property>
      <sv:property sv:name="hst:iconpath" sv:type="String">
        <sv:value>images/catalog-list.png</sv:value>
      </sv:property>
      <sv:property sv:name="hst:label" sv:type="String">
        <sv:value>List</sv:value>
      </sv:property>
      <sv:property sv:name="hst:parameternames" sv:type="String">
        <sv:value>title</sv:value>
        <sv:value>pageSize</sv:value>
        <sv:value>sortBy</sv:value>
      </sv:property>
      <sv:property sv:name="hst:parametervalues" sv:type="String">
        <sv:value>List</sv:value>
        <sv:value>3</sv:value>
        <sv:value>hippoplugins:date</sv:value>
      </sv:property>
      <sv:property sv:name="hst:template" sv:type="String">
        <sv:value>standard.main.list</sv:value>
      </sv:property>
      <sv:property sv:name="hst:xtype" sv:type="String">
        <sv:value>HST.Item</sv:value>
      </sv:property>
    </sv:node>

    * */



    public static void addToCatalog(final CatalogObject catalog, final PluginContext context) {
        try {
            final Session session = context.getSession();
            final Node siteRoot = session.getRootNode().getNode(HST_CONFIG_PATH);
            final Node site = siteRoot.getNode(catalog.getSiteName());
            final Node rootCatalogNode = site.getNode(CATALOG_PATH);
            final Node catalogNode = createEssentialsCatalogNode(rootCatalogNode);
            // TODO check if exists and create
            final String name = catalog.getName();
            if(catalogNode.hasNode(name)){
                log.warn("Catalog: component node already exists {}", catalogNode);
                return;
            }
            final Node component = catalogNode.addNode(name, CatalogObject.PRIMARY_TYPE);
            component.setProperty("hst:componentclassname", catalog.getComponentClassName());
            component.setProperty("hst:iconpath", catalog.getIconPath());
            component.setProperty("hst:label", catalog.getLabel());
            component.setProperty("hst:template", catalog.getTemplate());
            component.setProperty("hst:xtype", catalog.getType());
            final Map<String,String> parameters = catalog.getParameters();
            final Set<String> names = parameters.keySet();
            final String[] paramNames = names.toArray(new String[names.size()]);
            component.setProperty("hst:parameternames", paramNames);
            final Collection<String> values = parameters.values();
            final String[] paramValues = values.toArray(new String[values.size()]);
            component.setProperty("hst:parametervalues", paramValues);
            session.save();

        } catch (RepositoryException e) {
            log.error("Error writing nodes", e);
        }

    }

    public static void removeFromCatalog(final CatalogObject catalog, final PluginContext context) {
        try {
            final Session session = context.getSession();
            final Node siteRoot = session.getRootNode().getNode(HST_CONFIG_PATH);
            final Node site = siteRoot.getNode(catalog.getSiteName());
            final Node rootCatalogNode = site.getNode(CATALOG_PATH);
            final Node catalogNode = createEssentialsCatalogNode(rootCatalogNode);
            final String name = catalog.getName();
            if(catalogNode.hasNode(name)){
                //log.warn("Catalog: component node already exists {}", catalogNode);
                catalogNode.getNode(name).remove();
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("Error writing nodes", e);
        }
    }

    private static Node createEssentialsCatalogNode(final Node root) throws RepositoryException {
        if(root.hasNode(HIPPOESSENTIALS_CATALOG)){
            return root.getNode(HIPPOESSENTIALS_CATALOG);
        }
        // create one:
        final Node node = root.addNode(HIPPOESSENTIALS_CATALOG, "hst:containeritempackage");
        node.addMixin("mix:referenceable");
        return node;
    }


}
