/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.beanwriter.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.List;


import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
/*import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;*/
/*import org.onehippo.cms7.services.contenttype.HippoContentTypeService;*/
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: BeanwriterPlugin.java 173135 2013-08-08 08:25:11Z mmilicevic $"
 */
public class BeanwriterPlugin extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(BeanwriterPlugin.class);

    /**
     *  do not serialize
     */
    final transient Path namespacePath;

    public BeanwriterPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);

        final Form<?> form = new Form("form");
        final String basePath = ProjectUtils.getBaseProjectDirectory();

        namespacePath = new File(basePath + File.separator + "bootstrap").toPath();

        final AjaxButton executeButton = new AjaxButton("execute") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final List<MemoryBean> memoryBeans = BeanWriterUtils.buildBeansGraph(namespacePath, context, EssentialConst.SOURCE_PATTERN_JAVA);
                BeanWriterUtils.addMissingMethods(context, memoryBeans, EssentialConst.FILE_EXTENSION_JAVA);

            }
        };
        form.add(executeButton);
        add(form);



    }

}
