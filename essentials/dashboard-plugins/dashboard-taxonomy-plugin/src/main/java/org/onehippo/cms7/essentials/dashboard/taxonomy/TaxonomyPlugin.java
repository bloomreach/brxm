package org.onehippo.cms7.essentials.dashboard.taxonomy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.login.Configuration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.eclipse.jface.text.templates.TemplateException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.taxonomy.installer.TaxonomyInstaller;
import org.onehippo.cms7.essentials.dashboard.taxonomy.model.TaxonomyModel;
import org.onehippo.cms7.essentials.dashboard.taxonomy.util.HasNotTaxonomyMatcher;
import org.onehippo.cms7.essentials.dashboard.taxonomy.util.HasTaxonomyMatcher;
import org.onehippo.cms7.essentials.dashboard.taxonomy.util.TaxonomyQueryBuilder;
import org.onehippo.cms7.essentials.dashboard.ui.EssentialsFeedbackPanel;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.wicket.SortedTypeChoiceRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Jeroen Reijn
 */
public class TaxonomyPlugin extends InstallablePlugin<TaxonomyInstaller> {

    private final static Logger log = LoggerFactory.getLogger(TaxonomyPlugin.class);
    private static final long serialVersionUID = 1L;

    public static final String HIPPOTAXONOMY_TAXONOMY = "hippotaxonomy:taxonomy";
    public static final String HIPPOTAXONOMY_LOCALES = "hippotaxonomy:locales";
    public static final String HIPPOTAXONOMY_MIXIN = "hippotaxonomy:classifiable";

    private static final StringCodec codec = new StringCodecFactory.NameEncoding();

    private static final List<String> LOCALES = Arrays.asList("en", "fr", "es", "it", "nl");

    private List<String> selectedLocales = new ArrayList<>();
    private String taxonomyName;
    private TaxonomyModel selectedTaxonomy;
    private Prefer selected = Prefer.RIGHT;
    private transient Session session;

    public TaxonomyPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final FeedbackPanel feedbackPanel = new EssentialsFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        session = context.getSession();

        final DropDownChoice<TaxonomyModel> taxonomy = new DropDownChoice<>("taxonomy", new PropertyModel<TaxonomyModel>(this, "selectedTaxonomy"), populateTaxonomyModelList(), new ChoiceRenderer<TaxonomyModel>("name"));
        taxonomy.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                //empty
            }
        });
        taxonomy.setOutputMarkupId(true);
        add(taxonomy);

        final DropDownChoice<Prefer> prefer = new DropDownChoice<>("prefer", new PropertyModel<Prefer>(this, "selected"), Arrays.asList(Prefer.RIGHT, Prefer.LEFT), new IChoiceRenderer<Prefer>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(final Prefer object) {
                return object;
            }

            @Override
            public String getIdValue(final Prefer object, final int index) {
                return object.getPrefer();
            }
        });

        prefer.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                //empty
            }
        });
        prefer.setOutputMarkupId(true);
        add(prefer);

        final Form<?> form = new Form("form");
        TextField<String> myTaxonomyName = new TextField<>("defaultTaxonomyName", new PropertyModel<String>(this, "taxonomyName"));

        CheckGroup<String> group = new CheckGroup<>("group", new PropertyModel<List<String>>(this, "selectedLocales"));

        group.add(new CheckGroupSelector("groupselector"));

        ListView<String> locales = new ListView<String>("locales", LOCALES) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final IModel<String> model = item.getModel();
                item.add(new Check<>("checkbox", model));
                item.add(new Label("label", ' ' + model.getObject()));
            }
        };

        locales.setReuseItems(true);
        group.add(locales);
        form.add(group);
        form.add(myTaxonomyName);

        final AjaxButton addButton = new AjaxButton("add") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                onFormSubmit(this, target);
                target.add(feedbackPanel);
                taxonomy.setChoices(populateTaxonomyModelList());
                target.add(taxonomy);

                //target.add(getContext().getFeedBackPanel());    global message
            }

        };
        form.add(addButton);
        add(form);

        final Session mySession = context.getSession();
        final List<String> primaryNodeTypes = new ArrayList<>();
        final List<String> available = new ArrayList<>();
        final List<String> toAdd = new ArrayList<>();

        try {
            available.addAll(HippoNodeUtils.getPrimaryTypes(mySession, new HasNotTaxonomyMatcher(), "new-document"));
            toAdd.addAll(HippoNodeUtils.getPrimaryTypes(mySession, new HasTaxonomyMatcher(), "new-document"));
            primaryNodeTypes.addAll(HippoNodeUtils.getPrimaryTypes(mySession, "new-document"));
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve node types", e);
        }

        SortedTypeChoiceRenderer renderer = new SortedTypeChoiceRenderer(context, this, primaryNodeTypes);

        final ListChoice<String> availableTypesListChoice = new ListChoice<>("available-types", available, renderer);
        availableTypesListChoice.setOutputMarkupId(true);
        final ListChoice<String> addToTypesListChoice = new ListChoice<>("add-to-types", toAdd, renderer);
        addToTypesListChoice.setOutputMarkupId(true);

        availableTypesListChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (selectedTaxonomy != null) {
                    final String input = availableTypesListChoice.getInput();
                    toAdd.add(input);
                    available.remove(input);
                    addToTypesListChoice.setChoices(toAdd);
                    target.add(availableTypesListChoice);
                    target.add(addToTypesListChoice);
                    addOrRemoveClassifiableMixin(toAdd, available);
                } else {
                    error("Please add and/or select a taxonomy");
                    target.add(feedbackPanel);
                }
            }
        });
        add(availableTypesListChoice);

        addToTypesListChoice.add(new AjaxEventBehavior("onchange") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (selectedTaxonomy != null) {
                    final String input = addToTypesListChoice.getInput();
                    available.add(input);
                    toAdd.remove(input);
                    availableTypesListChoice.setChoices(available);
                    target.add(availableTypesListChoice);
                    target.add(addToTypesListChoice);
                    addOrRemoveClassifiableMixin(toAdd, available);
                } else {
                    error("Please add and/or select a taxonomy");
                    target.add(feedbackPanel);
                }
            }
        });
        add(addToTypesListChoice);


        Form buttonForm = new Form("form-updater");

        final AjaxButton run = new AjaxButton("run") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                UpdateUtils.copyFromRegistryToQueue(getContext(), "taxonomy-doc-updater");
            }
        };

        final AjaxButton create = new AjaxButton("create") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                TaxonomyQueryBuilder queryBuilder = new TaxonomyQueryBuilder.Builder().setDocumentTypes(toAdd).build();
                queryBuilder.addToRegistry(getContext());
                run.setEnabled(true);
                target.add(run);
            }
        };

        create.setOutputMarkupId(true);
        buttonForm.add(create);
        run.setEnabled(false);
        buttonForm.add(run);
        add(buttonForm);
        add(form);


    }

    private List<TaxonomyModel> populateTaxonomyModelList() {
        LinkedList<TaxonomyModel> models = new LinkedList<>();
        if (hasTaxonomyContainer()) {
            final Node taxonomyContainer = createOrGetTaxonomyContainer();
            try {
                final NodeIterator it = taxonomyContainer.getNodes();
                while (it.hasNext()) {
                    final Node taxonomyPlugin = it.nextNode();
                    final TaxonomyModel taxonomyModel = new TaxonomyModel(taxonomyPlugin);
                    models.add(taxonomyModel);
                }
            } catch (RepositoryException e) {
                log.error("", e);
            }
        }
        if (!models.isEmpty()) {
            selectedTaxonomy = models.getFirst();
        }
        return models;
    }

    @Override
    public TaxonomyInstaller getInstaller() {
        return new TaxonomyInstaller(getContext(), "http://www.hippoecm.org/hippotaxonomy/nt/1.2");
    }

    private boolean hasTaxonomyContainer() {
        try {
            return session.itemExists("/content/taxonomies") && session.getNode("/content/taxonomies").getPrimaryNodeType().getName().equals("hippotaxonomy:container");
        } catch (RepositoryException e) {
            log.error("Error: {}", e);
        }
        return false;
    }


    private Node createOrGetTaxonomyContainer() {
        Node taxonomiesNode = null;
        try {
            if (hasTaxonomyContainer()) {
                taxonomiesNode = session.getNode("/content/taxonomies");
            } else if (session.itemExists("/content")) {
                final Node contentNode = session.getNode("/content");
                taxonomiesNode = contentNode.addNode("taxonomies", "hippotaxonomy:container");
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("repository exception while trying to add or get the taxonomy container directory", e);
        }
        return taxonomiesNode;
    }


    private void onFormSubmit(final AjaxButton button, final AjaxRequestTarget target) {
        if (validate()) {
            try {
                final Node taxonomiesNode = createOrGetTaxonomyContainer();
                addDefaultTaxonomyNode(taxonomiesNode);
                session.save();
                onSuccess(button, target);
            } catch (Exception e) {
                log.error("An exception occurred while trying to save mixin: {}", e);
                onError(button, target);
            }

        }
    }

    private boolean validate() {
        return taxonomyNotNull() && !taxonomyExists() && localeSelected();
    }

    private boolean taxonomyExists() {
        try {
            if (session.itemExists("/content/taxonomies/" + codec.encode(taxonomyName))) {
                error(String.format("Taxonomy with name \"%s\" already exists. Please try another name", taxonomyName));
                return true;
            }
        } catch (RepositoryException e) {
            log.error("repository exception while trying to find out is given taxonomy exists", e);
        }
        return false;
    }

    private boolean localeSelected() {
        if (selectedLocales.isEmpty()) {
            error("No locale selected, please select at least one");
            return false;
        }
        return true;
    }

    private boolean taxonomyNotNull() {
        if (taxonomyName == null || StringUtils.isEmpty(taxonomyName)) {
            error("Please enter a taxonomy name");
            return false;
        }
        return true;
    }


    private void onError(final AjaxButton button, final AjaxRequestTarget target) {
        error("Error happened while trying to save taxonomy: " + taxonomyName);
    }

    private void onSuccess(final AjaxButton button, final AjaxRequestTarget target) {
        info("Added taxonomy document with name: " + taxonomyName);
    }


    private void addDefaultTaxonomyNode(final Node taxonomiesNode) throws RepositoryException {
        final Node handleNode = taxonomiesNode.addNode(codec.encode(taxonomyName), HippoNodeType.NT_HANDLE);
        handleNode.addMixin(HippoNodeType.NT_HARDHANDLE);

        final Node taxonomyNode = handleNode.addNode(codec.encode(taxonomyName), HIPPOTAXONOMY_TAXONOMY);
        taxonomyNode.addMixin(HippoNodeType.NT_HARDDOCUMENT);
        taxonomyNode.addMixin(HippoStdNodeType.NT_PUBLISHABLESUMMARY);

        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE, Calendar.getInstance());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE, Calendar.getInstance());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY, getUserName());
        taxonomyNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY, getUserName());

        taxonomyNode.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        taxonomyNode.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, getUserName());
        taxonomyNode.setProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY, "live");

        final Object[] locales = selectedLocales.toArray();
        taxonomyNode.setProperty(HIPPOTAXONOMY_LOCALES, Arrays.copyOf(locales, locales.length, String[].class));

    }

    /**
     * TODO refactor this and change template injection, probably broken right now....
     * @param toAdd
     * @param toRemove
     */
    private void addOrRemoveClassifiableMixin(final List<String> toAdd, final List<String> toRemove) {
        if (selectedTaxonomy == null) {
            throw new IllegalArgumentException("Taxonomy should not be empty");
        }
        InputStream in = null;

        for (String nodeType : toAdd) {
            try {
                Node docType;
                if (nodeType.contains(":")) {
                    docType = session.getNode("/hippo:namespaces/" + nodeType.replace(':', '/'));
                } else {
                    docType = session.getNode("/hippo:namespaces/system/" + nodeType);
                }
                // Add the mixin on the document type
                if (docType.canAddMixin(HIPPOTAXONOMY_MIXIN)) {
                    docType.addMixin(HIPPOTAXONOMY_MIXIN);
                }
                // Add the mixin on the hypposysedit:prototype node
                Node syseditNode = docType.getNode("hipposysedit:prototypes/hipposysedit:prototype");
                if (syseditNode.canAddMixin(HIPPOTAXONOMY_MIXIN)) {
                    syseditNode.addMixin(HIPPOTAXONOMY_MIXIN);
                }

                // Add the taxonomy field in the editing template by importing the XML
                if (docType.hasNode("editor:templates/_default_")) {
                    final Node defaultNode = docType.getNode("editor:templates/_default_");
                    if (defaultNode.hasNode("root") && !defaultNode.hasNode("taxonomy")) {
                        final Node root = defaultNode.getNode("root");
                        final String pluginClazz = root.getProperty("plugin.class").getString();
                        TaxonomyPlugin.PluginType pluginType = TaxonomyPlugin.PluginType.get(pluginClazz);
                        if (PluginType.UNKNOWN.equals(pluginType)) {
                            throw new IllegalArgumentException();
                        }

                        // Build the data-model
                        Map<String, Object> data = new HashMap<>();
                        String fieldType = "${cluster.id}.field";

                        if (pluginType.equals(PluginType.TWOCOLUMN)) {
                            switch (selected) {
                                case LEFT:
                                    fieldType = "${cluster.id}.left.item";
                                    break;
                                case RIGHT:
                                    fieldType = "${cluster.id}.right.item";
                                    break;
                            }
                        }


                        data.put("fieldType", fieldType);
                        //if (selectedTaxonomy != null) {
                        data.put("name", selectedTaxonomy.getName());
                        // }
                        final String parsed = TemplateUtils.injectTemplate("taxonomy.xml", data, getClass());
                        in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));
                        ((HippoSession) session).importDereferencedXML(defaultNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                                ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);
                    }
                }
                session.save();
                session.refresh(true);
            } catch (RepositoryException e) {
                error("Impossible to add the mixin to the following node: " + nodeType);
                log.error("Impossible to add the mixin to the following node: {}", nodeType, e);
            } catch (IOException e) {
                log.error("Error opening taxonomy.xml file", e);
            } catch (IllegalArgumentException e) {
                error("Impossible to add the mixin to the following node: " + nodeType);
                log.error("not able to add the nodetype");
            } finally {
                IOUtils.closeQuietly(in);

            }
        }

        for (String nodeType : toRemove) {
            try {
                Node docTypeNode;
                if (nodeType.contains(":")) {
                    docTypeNode = session.getNode("/hippo:namespaces/" + nodeType.replace(':', '/'));
                } else {
                    docTypeNode = session.getNode("/hippo:namespaces/system/" + nodeType);
                }
                // Remove the mixin from the document type
                if (docTypeNode.getMixinNodeTypes() != null && docTypeNode.getMixinNodeTypes().length > 0) {
                    for (NodeType nt : docTypeNode.getMixinNodeTypes()) {
                        if (nt.getName().equals(HIPPOTAXONOMY_MIXIN)) {
                            docTypeNode.removeMixin(HIPPOTAXONOMY_MIXIN);
                        }
                    }
                }

                // Remove the mixin on the hypposysedit:prototype node
                Node syseditNode = docTypeNode.getNode("hipposysedit:prototypes/hipposysedit:prototype");
                if (syseditNode.getMixinNodeTypes() != null && syseditNode.getMixinNodeTypes().length > 0) {
                    for (NodeType nt : syseditNode.getMixinNodeTypes()) {
                        if (nt.getName().equals(HIPPOTAXONOMY_MIXIN)) {
                            syseditNode.removeMixin(HIPPOTAXONOMY_MIXIN);
                        }
                    }
                }

                // Remove the taxonomy field from the editing template
                if (docTypeNode.hasNode("editor:templates/_default_")) {
                    final Node defaultNode = docTypeNode.getNode("editor:templates/_default_");
                    if (defaultNode.hasNode("taxonomy")) {
                        Node taxonomyNode = defaultNode.getNode("taxonomy");
                        taxonomyNode.remove();
                    }
                }
                session.save();
                session.refresh(true);
            } catch (RepositoryException e) {
                error("Impossible to remove the mixin to the following node: " + nodeType);
                log.error("Impossible to remove the mixin to the following node: {}", nodeType, e);
            }
        }
    }

    private String getUserName() {
        return "admin";
    }

    public enum Prefer implements Serializable {
        LEFT("left"), RIGHT("right");

        String prefer;

        private Prefer(String prefer) {
            this.prefer = prefer;
        }

        public String getPrefer() {
            return prefer;
        }
    }

    public enum PluginType {

        LISTVIEWPLUGIN("org.hippoecm.frontend.service.render.ListViewPlugin"), TWOCOLUMN("org.hippoecm.frontend.editor.layout.TwoColumn"), UNKNOWN("unknown");

        String clazz;

        PluginType(String clazz) {
            this.clazz = clazz;
        }

        public String getClazz() {
            return clazz;
        }

        public static PluginType get(String clazz) {
            for (PluginType a : PluginType.values()) {
                if (a.clazz.equals(clazz)) {
                    return a;
                }
            }
            return UNKNOWN;
        }

    }
}
