/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.setup;

import java.util.List;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.LogEvent;
import org.onehippo.cms7.essentials.dashboard.event.LoggingPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.MessageEvent;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.onehippo.cms7.essentials.setup.panels.ProjectSetupStep;
import org.onehippo.cms7.essentials.setup.panels.WelcomeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
public class SetupPage extends WebPage implements IHeaderContributor {


    private static final long serialVersionUID = 1L;
    private static final Predicate<Plugin> MAIN_PLUGIN = new Predicate<Plugin>() {
        @Override
        public boolean apply(final Plugin plugin) {
            final String type = plugin.getType();
            return type != null && "system".contains(type);
        }
    };
    private static Logger log = LoggerFactory.getLogger(SetupPage.class);
    private final ImmutableList<Plugin> mainPlugins;
    private final ImmutableList<Plugin> pluginList;
    @Inject
    private transient EventBus eventBus;
    @Inject
    private LoggingPluginEventListener listener;

    public SetupPage(final PageParameters parameters) {
        super(parameters);
        eventBus.post(new LogEvent("@@@@@@@@@@@@  Starting setup page @@@@@@@@@@@@@@"));
        //############################################
        // scan plugins
        //############################################
        final PluginScanner scanner = new PluginScanner();

        final ServletContext servletContext = WebApplication.get().getServletContext();
        final String libPath = servletContext.getRealPath("/WEB-INF/lib");
        log.info("Scanning path for essentials: {}", libPath);
        final List<Plugin> plugins = scanner.scan(libPath);
        for (Plugin plugin : plugins) {
            log.info("Found plugin: {}", plugin);
        }


        // filter system essentials:
        mainPlugins = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.and(MAIN_PLUGIN)));
        pluginList = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.not(MAIN_PLUGIN)));

        final AjaxWizardPanel wizard = new AjaxWizardPanel("wizard");
        wizard.addWizard(new WelcomeStep("Hippo Essentials setup"));
        Plugin plugin = getPluginByName("Settings");
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin, eventBus);
        wizard.addWizard(new ProjectSetupStep("Setup project", plugin, context));
        add(wizard);
    }

    private Plugin getPluginByName(String name) {
        Plugin plugin = null;
        for (final Plugin next : mainPlugins) {
            if (next.getName().equals(name)) {
                plugin = next;
            }
        }
        if (plugin != null) {
            return plugin;
        } else {
            for (final Plugin next : pluginList) {
                if (next.getName().equals(name)) {
                    plugin = next;
                }
            }
            return plugin;
        }
    }

}
