package org.onehippo.cms7.essentials.installer;

import java.util.List;

import javax.servlet.ServletContext;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeroen Reijn
 */
public class PluginHelper {

    private final static Logger log = LoggerFactory.getLogger(PluginHelper.class);
    final static PluginScanner scanner = new PluginScanner();

    private PluginHelper() {}

    public static List<Plugin> getPluginsFromServletContext(ServletContext context) {
        final String libPath = context.getRealPath("/WEB-INF/lib");
        log.info("Scanning path for essentials: {}", libPath);
        final List<Plugin> plugins = scanner.scan(libPath);
        for (Plugin plugin : plugins) {
            log.info("Found plugin: {}", plugin);
        }
        return plugins;
    }
}
