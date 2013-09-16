/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.Asset;
import org.onehippo.cms7.essentials.dashboard.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.CatalogObject;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstTemplate;
import org.onehippo.cms7.essentials.dashboard.utils.ComponentsUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HstUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id: ComponentsPlugin.java 164085 2013-05-13 10:05:42Z mmilicevic $"
 */
public class ComponentsPlugin extends InstallablePlugin<ComponentsInstaller> {

    public static final ImmutableBiMap<String, CatalogObject> COMPONENTS_MAPPING = new ImmutableBiMap.Builder<String, CatalogObject>()
            .put("Document component",
                    new CatalogObject("essentials-component-document", "Essentials Document Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsDocumentComponent")
                            .setIconPath("images/essentials/essentials-component-document.png")
                            .setType("HST.Item")
                            .setTemplate("hippo.essentials.components.document")

            )
            .put("Events component",
                    new CatalogObject("essentials-component-events", "Essentials Events Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsEventsComponent")
                            .setIconPath("images/essentials/essentials-component-events.png")
                            .setType("HST.Item")
                            .setTemplate("hippo.essentials.components.events")

            ).put("List component",
                    new CatalogObject("essentials-component-list", "Essentials List Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsListComponent")
                            .setIconPath("images/essentials/essentials-component-list.png")
                            .setType("HST.Item")
                            .setTemplate("hippo.essentials.components.list")

            )



                    /*.put("Events component", "org.onehippo.cms7.essentials.components.EssentialsEventsComponent")
                    .put("News component", "org.onehippo.cms7.essentials.components.EssentialsNewsComponent")
                    .put("List component", "org.onehippo.cms7.essentials.components.EssentialsDocumentListComponent")*/
            .build();
    // TODO refactor / make dynamic
    public static final String JSP_FOLDER = File.separator + "essentials" + File.separator + "components" + File.separator;
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ComponentsPlugin.class);
    final ListChoice<String> sitesChoice;
    private String selectedSite;
    private List<String> selectedComponents = new ArrayList<>();

    public ComponentsPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final Form<?> form = new Form("form");


        //############################################
        // SITES SELECT
        //############################################


        final List<String> sites = ComponentsUtils.getAllAvailableSites(context);

        final PropertyModel<String> siteModel = new PropertyModel<>(this, "selectedSite");
        sitesChoice = new ListChoice<>("sites", siteModel, sites);
        sitesChoice.setOutputMarkupId(true);

        //############################################
        // CHECKBOXES
        //############################################

        final CheckGroup<String> group = new CheckGroup<>("group", new PropertyModel<List<String>>(this, "selectedComponents"));
        group.add(new CheckGroupSelector("groupselector"));
        final ImmutableSet<String> componentSet = COMPONENTS_MAPPING.keySet();
        List<String> componentNames = new ArrayList<>(componentSet);
        final ListView<String> components = new ListView<String>("components", componentNames) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final IModel<String> model = item.getModel();
                item.add(new Check<>("checkbox", model));
                item.add(new Label("label", ' ' + model.getObject()));
            }
        };
        final AjaxButton saveButton = new

                AjaxButton("save") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        super.onSubmit(target, form);
                        onFormSubmit(this, target);
                    }
                };
        components.setReuseItems(true);
        group.add(sitesChoice);
        group.add(components);
        form.add(group);
        form.add(saveButton);
        add(form);

    }

    private void onFormSubmit(final AjaxButton button, final AjaxRequestTarget target) {

        if (Strings.isNullOrEmpty(selectedSite)) {
            // TODO add error
            return;
        }

        final PluginContext context = getContext();
        // TODO: check if component exists:
        for (String selectedComponent : selectedComponents) {
            final CatalogObject object = COMPONENTS_MAPPING.get(selectedComponent);
            object.setSiteName(selectedSite);
            ComponentsUtils.addToCatalog(object, getContext());
            // copy icons:
            final File folder = ProjectUtils.getSiteImagesFolder();
            if (folder.exists()) {
                final File essentials = FileUtils.getFile(folder, "essentials");
                if (!essentials.exists()) {
                    essentials.mkdir();
                }
                final Asset asset = context.getDescriptor().getAsset(selectedComponent);
                if (asset != null && !Strings.isNullOrEmpty(asset.getUrl())) {
                    final File file = new File(essentials.getAbsoluteFile() + File.separator + asset.getUrl());
                    copyAsset(file, asset);
                }
            }
            // copy JSP:
            final File siteJspFolder = ProjectUtils.getSiteJspFolder();
            final String templateName = object.getName() + ".jsp";
            final Asset asset = context.getDescriptor().getAsset(templateName);
            if (asset != null && !Strings.isNullOrEmpty(asset.getUrl())) {
                final File file = new File(siteJspFolder.getAbsoluteFile() + JSP_FOLDER + asset.getUrl());
                copyAsset(file, asset);
            }
            final HstTemplate template = new HstTemplate(object.getTemplate());
            template.setNamed(true);
            try {
                template.setRenderPath("jsp/essentials/components/" + templateName);
                final Session session = context.getSession();
                HstUtils.addTemplateNodeToConfiguration(session, selectedSite, template);
                session.save();

            } catch (RepositoryException e) {
                log.error("Error writing template", e);
            }


        }


    }

    private void copyAsset(final File target, final Asset asset) {
        final InputStream resourceAsStream = getClass().getResourceAsStream(asset.getUrl());
        if (resourceAsStream != null) {
            try {
                if (!target.exists()) {
                    final File parent = target.getParentFile();
                    Files.createDirectories(parent.toPath());
                }

                Files.copy(resourceAsStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Error writing file", e);
            }
        }
    }

    @Override
    public ComponentsInstaller getInstaller() {
        return new ComponentsInstaller();
    }

}
