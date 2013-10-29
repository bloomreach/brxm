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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.LogEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.LoggingPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.ValidationEventListener;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.onehippo.cms7.essentials.setup.panels.FinalStep;
import org.onehippo.cms7.essentials.setup.panels.SelectPowerpackStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.List;

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

    final ImmutableList<Plugin> mainPlugins;
    private final ImmutableList<Plugin> pluginList;
    @Inject
    private EventBus eventBus;
    @Inject
    private LoggingPluginEventListener loggingPluginEventListener;
    @Inject
    private MemoryPluginEventListener memoryPluginEventListener;
    @Inject
    private ValidationEventListener validationEventListener;


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
            eventBus.post(new LogEvent(String.format("@@@Found plugin: %s", plugin)));
            eventBus.post(new DisplayEvent(String.format("@@@Found plugin: %s", plugin)));
        }


        // filter system essentials:
        mainPlugins = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.and(MAIN_PLUGIN)));
        pluginList = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.not(MAIN_PLUGIN)));

        //############################################
        // SETTINGS PLUGIN TODO: do we need this one?
        //############################################

        Plugin plugin = getPluginByName("Settings");
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
        //############################################
        // WIZARD & STEPS
        //############################################
        final AjaxWizardPanel wizard = new AjaxWizardPanel("wizard");
        wizard.addWizard(new SelectPowerpackStep(getString("step.choose.powerpack")));
        wizard.addWizard(new FinalStep(getString("step.overview")));
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
