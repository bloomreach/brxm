/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.installer;

import java.util.List;

import javax.jcr.Session;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * @version "$Id: BlockingModalPanel.java 164013 2013-05-11 14:05:39Z mmilicevic $"
 */
public class BlockingModalPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(BlockingModalPanel.class);
    private String beansPackage;
    private String componentsPackage;

    public BlockingModalPanel(String id, PluginContext context) {
        super(id);
        final Session session = context.getSession();
        final List<String> projectNamespaces;
        final List<String> sitePackages;

        // for easier component testing
        if (session != null) {
            projectNamespaces = HippoNodeUtils.getProjectNamespaces(session);
            sitePackages = ProjectUtils.getSitePackages(context);
        } else {
            projectNamespaces = ImmutableList.of("no-session");
            sitePackages = ImmutableList.of("com.no.session");
        }
        final Form<?> form = new Form<Object>("form");
        final DropDownChoice<String> pluginNamespace = new DropDownChoice<>("projectNamespace", projectNamespaces);
        final DropDownChoice<String> componentPackage = new DropDownChoice<>("componentPackage", new PropertyModel<String>(this, "componentsPackage"), sitePackages);
        final DropDownChoice<String> beanPackage = new DropDownChoice<>("beanPackage", new PropertyModel<String>(this, "beansPackage"), sitePackages);


        final WebMarkupContainer table = new WebMarkupContainer("table");


        //############################################
        // ADD ITEMS
        //############################################
        form.add(table);
        table.add(pluginNamespace);
        table.add(beanPackage);
        table.add(componentPackage);
        add(form);

    }
}
