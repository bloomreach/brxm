/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.beanwriter.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.BeanWriterLogEntry;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*import org.onehippo.cms7.services.contenttype.DocumentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;*/
/*import org.onehippo.cms7.services.contenttype.HippoContentTypeService;*/

/**
 * @version "$Id: BeanwriterPlugin.java 173135 2013-08-08 08:25:11Z mmilicevic $"
 */
public class BeanwriterPlugin extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(BeanwriterPlugin.class);
    /**
     * do not serialize
     */
    final transient Path namespacePath;
    private final BeanWriterActionPanel logsPanel;

    public BeanwriterPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        logsPanel = new BeanWriterActionPanel("logsPanel", Collections.<BeanWriterLogEntry>emptyList());

        final String basePath = ProjectUtils.getBaseProjectDirectory();

        namespacePath = new File(basePath + File.separator + "bootstrap").toPath();

/*
        final AjaxButton executeButton = new AjaxButton("execute") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final List<MemoryBean> memoryBeans = BeanWriterUtils.buildBeansGraph(namespacePath, context, EssentialConst.SOURCE_PATTERN_JAVA);
                BeanWriterUtils.addMissingMethods(context, memoryBeans, EssentialConst.FILE_EXTENSION_JAVA);
                final Collection<Object> pluginContextData = context.getPluginContextData(BeanWriterUtils.CONTEXT_DATA_KEY);
                final List<BeanWriterLogEntry> entries = new ArrayList<>();
                for (Object o : pluginContextData) {
                    entries.add((BeanWriterLogEntry) o);
                }
                // repaint panel:
                logsPanel.replaceModel(entries);

            }
        };
*/

        setOutputMarkupId(true);

    }


}
