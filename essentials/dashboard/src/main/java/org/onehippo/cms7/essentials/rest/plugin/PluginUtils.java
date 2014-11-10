//package org.onehippo.cms7.essentials.rest.plugin;
//
//import org.apache.commons.collections.CollectionUtils;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
//import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
//import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
//import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
//import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
//import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
//import org.onehippo.cms7.essentials.rest.model.RestList;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.text.MessageFormat;
//import java.util.*;
//
///**
// * Created by tjeger on 10/11/14.
// */
//public class PluginUtils {
//
//    public static List<Plugin> getAllPlugins() {
//        final RestfulList<PluginDescriptorRestful> plugins = new RestList<>();
//        final List<PluginDescriptorRestful> items = getLocalPlugins();
//        final Collection<String> restClasses = new ArrayList<>();
//        final PluginContext context = PluginContextFactory.getContext();
//        // remote plugins
//        final ProjectSettings projectSettings = getProjectSettings(servletContext);
//        final Set<String> pluginRepositories = projectSettings.getPluginRepositories();
//        for (String pluginRepository : pluginRepositories) {
//            try {
//                final RestfulList<PluginDescriptorRestful> myPlugins = pluginCache.get(pluginRepository);
//                log.debug("{}", pluginCache.stats());
//                if (myPlugins != null) {
//                    final List<PluginDescriptorRestful> myPluginsItems = myPlugins.getItems();
//                    CollectionUtils.addAll(items, myPluginsItems.iterator());
//                }
//            } catch (Exception e) {
//                log.error(MessageFormat.format("Error loading plugins from repository: {0}", pluginRepository), e);
//            }
//
//        }
//
//        processPlugins(plugins, items, restClasses);
//
//        //############################################
//        // Register endpoints:
//        //############################################
//        registerEndpoints(restClasses);
//        return plugins;
//
//    }
//
//    private static List<Plugin> getLocalPlugins() {
//        final InputStream stream = PluginUtils.class.getResourceAsStream("/plugin_descriptor.json");
//        final String json = GlobalUtils.readStreamAsText(stream);
//        final ObjectMapper mapper = new ObjectMapper();
//        try {
//            @SuppressWarnings("unchecked")
//            final RestfulList<PluginDescriptorRestful> restfulList = mapper.readValue(json, RestfulList.class);
//            return restfulList.getItems();
//        } catch (IOException e) {
//            log.error("Error parsing plugins", e);
//        }
//        return Collections.emptyList();
//    }
//
//}
