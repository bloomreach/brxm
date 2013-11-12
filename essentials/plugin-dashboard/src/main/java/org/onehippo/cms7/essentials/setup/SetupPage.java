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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.ConfigDocument;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.LogEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.LoggingPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.ValidationEventListener;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.onehippo.cms7.essentials.installer.panels.GlobalToolbarPanel;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpack;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpackWithSamples;
import org.onehippo.cms7.essentials.setup.panels.FinalStep;
import org.onehippo.cms7.essentials.setup.panels.SelectPowerpackStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private EventBus eventBus;
    @Inject
    private LoggingPluginEventListener loggingPluginEventListener;
    @Inject
    private MemoryPluginEventListener memoryPluginEventListener;
    @Inject
    private ValidationEventListener validationEventListener;

    private final SelectPowerpackStep selectStep;
    private final FinalStep finalStep;
    final PluginContext dashboardPluginContext;

    @SuppressWarnings("unchecked")
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
        }


        // filter system essentials:
        mainPlugins = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.and(MAIN_PLUGIN)));
        pluginList = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.not(MAIN_PLUGIN)));


        Plugin plugin = getPluginByName("Settings");
        dashboardPluginContext = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
        //############################################
        // INJECT PROJECT SETTINGS
        //############################################
        final ConfigDocument document = dashboardPluginContext.getConfigService().read(ProjectSetupPlugin.class.getName());
        if (document != null) {
            dashboardPluginContext.setBeansPackageName(document.getValue(ProjectSetupPlugin.PROPERTY_BEANS_PACKAGE));
            dashboardPluginContext.setComponentsPackageName(document.getValue(ProjectSetupPlugin.PROPERTY_COMPONENTS_PACKAGE));
            dashboardPluginContext.setRestPackageName(document.getValue(ProjectSetupPlugin.PROPERTY_REST_PACKAGE));
            dashboardPluginContext.setProjectNamespacePrefix(document.getValue(ProjectSetupPlugin.PROPERTY_NAMESPACE));
        }

        final IndicatingAjaxLink autoExportLink = new IndicatingAjaxLink("autoexportLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                enableAutoExport();
                setEnabled(false);
                setVisible(false);
                target.add(this);
            }
        };

        //Preliminatory checks: is auto-export enabled ?
        if (!autoExportEnabled()) {
            warn("Autoexport is not enabled, configuration changes may be lost");
        } else {
            autoExportLink.setVisible(false);
        }

        add(autoExportLink);

        //############################################
        // WIZARD & STEPS
        //############################################
        final AjaxWizardPanel wizard = new AjaxWizardPanel("wizard") {
            @Override
            public void onFinish() {
                log.debug("Selected Power Pack is {}", selectStep.getSelectedPowerpack());
                final PowerpackPackage powerpackPackage;
                switch (selectStep.getSelectedPowerpack()) {
                    case SelectPowerpackStep.POWERPACK_NEWS_AND_EVENT_LABEL:
                        if (selectStep.isInstallSampleContentChecked()) {
                            powerpackPackage = new BasicPowerpackWithSamples();
                        } else {
                            powerpackPackage = new BasicPowerpack();
                        }
                        break;
                    default:
                        powerpackPackage = new EmptyPowerPack();
                        break;
                }
                final InstructionStatus status = powerpackPackage.execute(dashboardPluginContext);
                switch (status) {
                    case SUCCESS:
                        info("Installation finished successfully (" + status + ")");
                        break;
                    case FAILED:
                        warn("Installation failed");

                }

            }
        };

        selectStep = new SelectPowerpackStep(this, getString("step.choose.powerpack"));
        finalStep = new FinalStep(this, getString("step.overview"));
        wizard.addWizard(selectStep);
        wizard.addWizard(finalStep);
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

    public FinalStep getFinalStep() {
        return finalStep;
    }

    public SelectPowerpackStep getSelectStep() {
        return selectStep;
    }

    private boolean autoExportEnabled() {
        try {
            Session session = dashboardPluginContext.getSession();
            if (session !=null && session.nodeExists(GlobalToolbarPanel.AUTO_EXPORT_PATH)) {
                final Node autoExportNode = session.getNode(GlobalToolbarPanel.AUTO_EXPORT_PATH);
                if (autoExportNode.hasProperty(GlobalToolbarPanel.AUTOEXPORT_ENABLED)) {
                    return autoExportNode.getProperty(GlobalToolbarPanel.AUTOEXPORT_ENABLED).getBoolean();
                }
            }

        } catch (Exception e) {
            log.error("Error checking auto export availability", e);
        }
        return false;
    }

    private void enableAutoExport() {
        try {
            Session session = dashboardPluginContext.getSession();
            if (session !=null && session.nodeExists(GlobalToolbarPanel.AUTO_EXPORT_PATH)) {
                final Node autoExportNode = session.getNode(GlobalToolbarPanel.AUTO_EXPORT_PATH);
                if (autoExportNode.hasProperty(GlobalToolbarPanel.AUTOEXPORT_ENABLED)) {
                    autoExportNode.setProperty(GlobalToolbarPanel.AUTOEXPORT_ENABLED, Boolean.TRUE);
                    session.save();
                }
            }
        } catch (Exception e) {
            log.error("Error enabling auto export", e);
        }
    }

    private class EmptyPowerPack implements PowerpackPackage {
        @Override
        public Instructions getInstructions() {
            return new Instructions() {
                @Override
                public Set<InstructionSet> getInstructionSets() {
                    return Collections.emptySet();
                }

                @Override
                public void setInstructionSets(Set<InstructionSet> instructionSets) {
                }
            };
        }

        @Override
        public InstructionStatus execute(PluginContext context) {
            return InstructionStatus.SUCCESS;
        }
    }
}
