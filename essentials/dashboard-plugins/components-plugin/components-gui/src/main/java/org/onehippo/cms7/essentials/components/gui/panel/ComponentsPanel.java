/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components.gui.panel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.components.gui.ComponentsWizard;
import org.onehippo.cms7.essentials.dashboard.Asset;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.CatalogObject;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstTemplate;
import org.onehippo.cms7.essentials.dashboard.model.hst.TemplateExistsException;
import org.onehippo.cms7.essentials.dashboard.ui.EssentialsFeedbackPanel;
import org.onehippo.cms7.essentials.dashboard.utils.ComponentsUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HstUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;

/**
 * @version "$Id$"
 */
public class ComponentsPanel extends EssentialsWizardStep {

    public static final ImmutableBiMap<String, CatalogObject> COMPONENTS_MAPPING = new ImmutableBiMap.Builder<String, CatalogObject>()
            .put("Document Component",
                    new CatalogObject("essentials-component-document", "Essentials Document Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsDocumentComponent")
                            .setIconPath("images/essentials/essentials-component-document.png")
                            .setType("HST.Item")
                            .setTemplate("hippo.essentials.components.document")

            )
            .put("Events Component",
                    new CatalogObject("essentials-component-events", "Essentials Events Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsEventsComponent")
                            .setIconPath("images/essentials/essentials-component-events.png")
                            .setType("HST.Item")
                            .setTemplate("hippo.essentials.components.events")

            ).put("List Component",
                    new CatalogObject("essentials-component-list", "Essentials List Component")
                            .setComponentClassName("org.onehippo.cms7.essentials.components.EssentialsListComponent")
                            .setIconPath("images/essentials/essentials-component-list.png")
                            .setType("HST.Item")
                            .setTemplate("hippo.essentials.components.list")

            )
            .build();
    // TODO refactor / make dynamic
    public static final String JSP_FOLDER = File.separator + "essentials" + File.separator + "components" + File.separator;
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ComponentsPanel.class);
    private final ListChoice<String> sitesChoice;
    private final ListMultipleChoice<String> availableTypesListChoice;
    private final ListMultipleChoice<String> addToTypesListChoice;
    private final PluginContext context;
    private final List<String> available;
    private final List<String> toAdd;
    private String selectedSite;

    public ComponentsPanel(final ComponentsWizard parent, final String id) {
        super(id);
        context = parent.getContext();
        final FeedbackPanel feedbackPanel = new EssentialsFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final Form<?> form = new Form("form");
        //############################################
        // SITES SELECT
        //############################################
        final List<String> sites = ComponentsUtils.getAllAvailableSites(context);

        final PropertyModel<String> siteModel = new PropertyModel<>(this, "selectedSite");
        sitesChoice = new ListChoice<>("sites", siteModel, sites);
        sitesChoice.setNullValid(false);
        sitesChoice.setOutputMarkupId(true);

        sitesChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {

                final String input = sitesChoice.getInput();
                if (StringUtils.isNotEmpty(input) && StringUtils.isNumeric(input)) {
                    final String site = sitesChoice.getChoices().get(Integer.parseInt(input));
                    selectedSite = site;
                    final List<String> addedComponents = Util.getAddedComponents(context, site);

                    addToTypesListChoice.setChoices(addedComponents);
                    availableTypesListChoice.setChoices(Util.getAvailableComponents(COMPONENTS_MAPPING.keySet(), addedComponents));

                    target.add(addToTypesListChoice, availableTypesListChoice);

                } else {
                    availableTypesListChoice.setChoices(Collections.<String>emptyList());
                    addToTypesListChoice.setChoices(Collections.<String>emptyList());
                    target.add(availableTypesListChoice, addToTypesListChoice);
                }
            }
        });
        form.add(sitesChoice);

        //############################################
        // Component add components
        //############################################


        available = new ArrayList<>();

        toAdd = new ArrayList<>();


        final PropertyModel<List<String>> availableModel = new PropertyModel<>(this, "available");
        availableTypesListChoice = new ListMultipleChoice<>("available-types", availableModel, available);
        availableTypesListChoice.setOutputMarkupId(true);

        final PropertyModel<List<String>> addToModel = new PropertyModel<>(this, "toAdd");
        addToTypesListChoice = new ListMultipleChoice<>("add-to-types", addToModel, toAdd);
        addToTypesListChoice.setOutputMarkupId(true);

        availableTypesListChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (selectedSite != null) {
                    @SuppressWarnings("unchecked")
                    final List<String> availableChoices = (List<String>) availableTypesListChoice.getChoices();
                    final String input = availableChoices.get(Integer.parseInt(availableTypesListChoice.getInput()));
                    final boolean b = addCatalogToSite(context, input, selectedSite);
                    final List<String> addedComponents = Util.getAddedComponents(context, selectedSite);
                    addToTypesListChoice.setChoices(addedComponents);
                    availableTypesListChoice.setChoices(Util.getAvailableComponents(COMPONENTS_MAPPING.keySet(), addedComponents));

                    target.add(availableTypesListChoice, addToTypesListChoice);
                } else {
                    error("Please add and/or select a site");
                    target.add(feedbackPanel);
                }
            }
        });
        form.add(availableTypesListChoice);

        addToTypesListChoice.add(new AjaxEventBehavior("onchange") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (selectedSite != null) {
                    final String input = addToTypesListChoice.getChoices().get(Integer.parseInt(addToTypesListChoice.getInput()));

                    //todo some magic;
                    final boolean b = removeCatalogFromSite(context, input, selectedSite);

                    List<String> addedComponents = Util.getAddedComponents(context, selectedSite);


                    addToTypesListChoice.setChoices(addedComponents);
                    availableTypesListChoice.setChoices(Util.getAvailableComponents(COMPONENTS_MAPPING.keySet(), addedComponents));

                    target.add(availableTypesListChoice, addToTypesListChoice);
                } else {
                    error("Please add and/or select a site");
                    target.add(feedbackPanel);
                }
            }
        });
        form.add(addToTypesListChoice);

        add(form);

    }

    private boolean addCatalogToSite(final PluginContext context, final String input, final String selectedSite) {
        final CatalogObject catalogObject = COMPONENTS_MAPPING.get(input);
        catalogObject.setSiteName(selectedSite);
        ComponentsUtils.addToCatalog(catalogObject, context);

        final File folder = ProjectUtils.getSiteImagesFolder();
        if (folder.exists()) {
            final File essentials = FileUtils.getFile(folder, "essentials");
            if (!essentials.exists()) {
                essentials.mkdir();
            }
            final Asset asset = context.getDescriptor().getAsset(input);
            if (asset != null && !Strings.isNullOrEmpty(asset.getUrl())) {
                final File file = new File(essentials.getAbsoluteFile() + File.separator + asset.getUrl());
                copyAsset(file, asset);
            }
        }
        // copy JSP:
        final File siteJspFolder = ProjectUtils.getSiteJspFolder();
        final String templateName = catalogObject.getName() + ".jsp";
        final Asset asset = context.getDescriptor().getAsset(templateName);
        if (asset != null && !Strings.isNullOrEmpty(asset.getUrl())) {
            final File file = new File(siteJspFolder.getAbsoluteFile() + JSP_FOLDER + asset.getUrl());
            copyAsset(file, asset);
        }
        final HstTemplate template = new HstTemplate(catalogObject.getTemplate());
        template.setNamed(true);
        try {
            template.setRenderPath("jsp/essentials/components/" + templateName);
            final Session session = context.getSession();
            HstUtils.addTemplateNodeToConfiguration(session, selectedSite, template);
            session.save();
            return true;
        } catch (RepositoryException e) {
            log.error("Error writing template", e);
        } catch (TemplateExistsException e) {
            log.warn("template already exists.", e);
            error("template already exists.");
            return true;
        }
        return false;
    }

    private boolean removeCatalogFromSite(final PluginContext context, final String input, final String selectedSite) {
        final CatalogObject catalogObject = COMPONENTS_MAPPING.get(input);
        catalogObject.setSiteName(selectedSite);
        ComponentsUtils.removeFromCatalog(catalogObject, context);
        return true;
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

    /**
     * Component installer utils
     */
    public static final class Util {

        public static final String CATALOG_PATH = "hst:catalog";
        public static final String HST_CONFIG_PATH = "hst:hst/hst:configurations";
        public static final String HIPPOESSENTIALS_CATALOG = "hippoessentials-catalog";
        public static final String HIPPOESSENTIALS_PREFIX = "Essentials ";

        private Util() {
        }

        public static List<String> getAddedComponents(PluginContext context, String siteName) {
            final List<String> addedComponents = new ArrayList<>();
            final Session session = context.getSession();
            try {
                final Node rootNode = session.getRootNode();
                final Node node = rootNode.getNode(HST_CONFIG_PATH);
                if (node.hasNode(siteName + '/' + CATALOG_PATH + '/' + HIPPOESSENTIALS_CATALOG)) {
                    //final Node site = node.getNode(siteName);
                    //todo get parent stuff..
                    final Node essentialsCatalog = node.getNode(siteName + '/' + CATALOG_PATH + '/' + HIPPOESSENTIALS_CATALOG);
                    final NodeIterator it = essentialsCatalog.getNodes();
                    while (it.hasNext()) {
                        final Node essentialContainerItem = it.nextNode();
                        final String label = essentialContainerItem.getProperty("hst:label").getString();
                        final String key = label.replace(HIPPOESSENTIALS_PREFIX, "");
                        if (COMPONENTS_MAPPING.containsKey(key)) {
                            addedComponents.add(key);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error fetching components", e);
            }
            return addedComponents;
        }

        public static List<String> getAvailableComponents(Collection<String> available, Collection<String> added) {
            List<String> abv = new ArrayList<>(available);
            abv.removeAll(added);
            return abv;
        }
    }
}
