package org.onehippo.cms7.essentials.dashboard.relateddocs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.relateddocs.installer.RelatedDocsInstaller;
import org.onehippo.cms7.essentials.dashboard.relateddocs.query.RelatedDocQueryBuilder;
import org.onehippo.cms7.essentials.dashboard.ui.EssentialsFeedbackPanel;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.update.UpdateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.wicket.SortedTypeChoiceRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class RelatedDocsPlugin extends InstallablePlugin<RelatedDocsInstaller> {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(RelatedDocsPlugin.class);

    private Prefer selected = Prefer.RIGHT;


    public RelatedDocsPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);

        final FeedbackPanel feedbackPanel = new EssentialsFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final Session session = context.getSession();

        final List<String> primaryNodeTypes = new ArrayList<>();
        final List<String> available = new ArrayList<>();
        final List<String> added = new ArrayList<>();

        try {
            available.addAll(HippoNodeUtils.getPrimaryTypes(session, new NotRelatedDocMatcher(), "new-document"));
            primaryNodeTypes.addAll(HippoNodeUtils.getPrimaryTypes(session, "new-document"));
            added.addAll(HippoNodeUtils.getPrimaryTypes(session, new HasRelatedDocMatcher(), "new-document"));
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve node types", e);
        }

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

        final SortedTypeChoiceRenderer renderer = new SortedTypeChoiceRenderer(context, this, primaryNodeTypes);


        final ListChoice<String> listMultipleChoice = new ListChoice<>("available", available, renderer);
        listMultipleChoice.setOutputMarkupId(true);
        final ListChoice<String> listAddedMultipleChoice = new ListChoice<>("added", added, renderer);
        listAddedMultipleChoice.setOutputMarkupId(true);


        listMultipleChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.add(feedbackPanel);
                final String input = listMultipleChoice.getInput();
                boolean relAdded = addRelatedDocsPluginToNodeType(input, selected);
                if (relAdded) {
                    added.add(input);
                    available.remove(input);
                    listAddedMultipleChoice.setChoices(added);
                    target.add(listMultipleChoice);
                    target.add(listAddedMultipleChoice);
                } else {
                    error("Unable to add related docs plugin to unsupported template: " + renderer.getDisplayValue(input));
                    target.add(feedbackPanel);
                }
            }
        });
        add(listMultipleChoice);

        listAddedMultipleChoice.add(new AjaxEventBehavior("onchange") {


            private static final long serialVersionUID = 1L;


            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.add(feedbackPanel);
                final String input = listAddedMultipleChoice.getInput();
                final boolean relRemoved = removeRelatedDocsPluginToNodeType(input);
                if (relRemoved) {
                    available.add(input);
                    added.remove(input);
                    listMultipleChoice.setChoices(available);
                    target.add(listMultipleChoice);
                    target.add(listAddedMultipleChoice);
                } else {
                    error("Unable to remove related docs from: " + renderer.getDisplayValue(input));
                    target.add(feedbackPanel);
                }
            }
        });
        add(listAddedMultipleChoice);

        Form<?> form = new Form("form");

        final AjaxButton run = new AjaxButton("run") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                UpdateUtils.copyFromRegistryToQueue(getContext(), "related-doc-updater");
            }
        };

        final AjaxButton create = new AjaxButton("create") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                RelatedDocQueryBuilder queryBuilder = new RelatedDocQueryBuilder.Builder().setDocumentTypes(added).build();
                queryBuilder.addToRegistry(getContext());
                run.setEnabled(true);
                target.add(run);
            }
        };

        create.setOutputMarkupId(true);
        form.add(create);
        run.setEnabled(false);
        form.add(run);
        add(form);

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


    public boolean addRelatedDocsPluginToNodeType(String type, Prefer prefer) {
        final Session session = getContext().getSession();
        InputStream in = null;
        try {
            Node docType;
            if (type.contains(":")) {
                docType = session.getNode("/hippo:namespaces/" + type.replace(':', '/'));
            } else {
                docType = session.getNode("/hippo:namespaces/system/" + type);
            }
            if (docType.hasNode("editor:templates/_default_/root")) {
                if (docType.hasNode("hipposysedit:prototypes/hipposysedit:prototype")) {
                    final Node prototype = docType.getNode("hipposysedit:prototypes/hipposysedit:prototype");
                    if (prototype.canAddMixin("relateddocs:relatabledocs")) {
                        prototype.addMixin("relateddocs:relatabledocs");
                    }
                }
                final Node root = docType.getNode("editor:templates/_default_/root");

                if (root.canAddMixin("relateddocs:relatabledocs")) {
                    root.addMixin("relateddocs:relatabledocs");
                } else {

                }

                if (root.hasProperty("plugin.class")) {
                    final String pluginClazz = root.getProperty("plugin.class").getString();
                    final HippoSession hippoSession = (HippoSession) session;
                    RelatedDocsPlugin.PluginType pluginType = RelatedDocsPlugin.PluginType.get(pluginClazz);
                    String absPath = docType.getNode("editor:templates").getPath();
                    boolean addedRelatedDocs = false;
                    switch (pluginType) {
                        case LISTVIEWPLUGIN:
                            in = getClass().getResourceAsStream("/listview.xml");
                            hippoSession.importDereferencedXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                            addedRelatedDocs = true;
                            break;
                        case TWOCOLUMN:
                            if (prefer == null) {
                                prefer = Prefer.RIGHT;
                            }
                            in = getClass().getResourceAsStream("/two_column_" + prefer.getPrefer() + ".xml");
                            hippoSession.importDereferencedXML(absPath, in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
                            addedRelatedDocs = true;
                            break;
                    }
                    hippoSession.save();
                    return addedRelatedDocs;
                }
            }
        } catch (RepositoryException | IOException e) {
            log.error("Error adding related doc nodes", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return false;
    }

    public boolean removeRelatedDocsPluginToNodeType(String type) {
        final Session session = getContext().getSession();
        try {
            Node docType;
            if (type.contains(":")) {
                docType = session.getNode("/hippo:namespaces/" + type.replace(':', '/'));
            } else {
                docType = session.getNode("/hippo:namespaces/system/" + type);
            }
            if (docType.hasNode("editor:templates/_default_")) {
                if (docType.hasNode("hipposysedit:prototypes/hipposysedit:prototype")) {
                    final Node prototype = docType.getNode("hipposysedit:prototypes/hipposysedit:prototype");
                    prototype.removeMixin("relateddocs:relatabledocs");
                }
                boolean hasRelatedDocs = false;
                final Node _default_ = docType.getNode("editor:templates/_default_");
                final NodeIterator it = _default_.getNodes();
                while (it.hasNext()) {
                    final Node node = it.nextNode();
                    if (node.hasProperty("plugin.class")) {
                        final String pluginClass = node.getProperty("plugin.class").getString();
                        if (pluginClass.equals("org.onehippo.forge.relateddocs.editor.RelatedDocsSuggestPlugin") || pluginClass.equals("org.onehippo.forge.relateddocs.editor.RelatedDocsPlugin")) {
                            hasRelatedDocs = true;
                            node.remove();
                        }
                    }
                }
                if (_default_.hasNode("translator/hippostd:translations")) {
                    final Node translations = _default_.getNode("translator/hippostd:translations");
                    final NodeIterator tit = translations.getNodes();
                    while (tit.hasNext()) {
                        final Node translation = tit.nextNode();
                        if (translation.getName().equals("relateddocs") || translation.getName().equals("relateddocssuggest")) {
                            translation.remove();
                        }
                    }
                }
                session.save();
                return hasRelatedDocs;
            }
        } catch (RepositoryException e) {
            log.error("Error removing related doc nodes", e);
        }
        return false;
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

    @Override
    public RelatedDocsInstaller getInstaller() {
        return new RelatedDocsInstaller(getContext(), "http://forge.onehippo.org/relateddocs/nt/1.1");
    }


}
