package org.onehippo.cms7.essentials.dashboard.contentblocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.eclipse.jface.text.templates.TemplateException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.contentblocks.installer.ContentBlocksInstaller;
import org.onehippo.cms7.essentials.dashboard.contentblocks.matcher.HasProviderMatcher;
import org.onehippo.cms7.essentials.dashboard.contentblocks.model.ContentBlockModel;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.ui.EssentialsFeedbackPanel;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.wicket.SortedTypeChoiceRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author wbarthet
 */
public class ContentBlocksPlugin extends InstallablePlugin<ContentBlocksInstaller> {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ContentBlocksPlugin.class);
    private Prefer selected = Prefer.RIGHT;
    private Type type = Type.DROPDOWN;
    private String name;
    private String provider;
    private ContentBlockModel contentblock;

    public ContentBlocksPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final FeedbackPanel feedbackPanel = new EssentialsFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final Session session = context.getSession();
        final List<String> primaryNodeTypes = new ArrayList<>();
        final List<String> available = new ArrayList<>();
        final List<ContentBlockModel> toAdd = new ArrayList<>();
        final List<String> providerList = new ArrayList<>();

        try {
            available.addAll(HippoNodeUtils.getPrimaryTypes(session, new HasProviderMatcher(), "new-document"));
            toAdd.addAll(getContentBlocks());
            primaryNodeTypes.addAll(HippoNodeUtils.getPrimaryTypes(session, "new-document"));
            providerList.addAll(HippoNodeUtils.getCompounds(session));
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve node types", e);
        }


        final SortedTypeChoiceRenderer renderer = new SortedTypeChoiceRenderer(context, this, primaryNodeTypes);
        final SortedTypeChoiceRenderer providerRender = new SortedTypeChoiceRenderer(context, this, providerList);

        final DropDownChoice<String> myProvider = new DropDownChoice<>("provider", new PropertyModel<String>(this, "provider"), providerList, providerRender);
        myProvider.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                //empty
            }
        });
        myProvider.setOutputMarkupId(true);
        add(myProvider);

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


        final DropDownChoice<Type> myType = new DropDownChoice<>("type", new PropertyModel<Type>(this, "type"), Arrays.asList(Type.DROPDOWN, Type.LINKS), new IChoiceRenderer<Type>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(final Type object) {
                return object;
            }

            @Override
            public String getIdValue(final Type object, final int index) {
                return object.getType();
            }
        });
        myType.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                //empty
            }
        });
        myType.setOutputMarkupId(true);
        add(myType);

        TextField<String> path = new TextField<>("name", new PropertyModel<String>(this, "name"));
        path.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                //empty
            }
        });
        path.setOutputMarkupId(true);
        add(path);


        final ListChoice<String> availableTypesListChoice = new ListChoice<>("available-types", available, renderer);
        availableTypesListChoice.setOutputMarkupId(true);

        final ListChoice<ContentBlockModel> addToTypesListChoice = new ListChoice<>("add-to-types", new PropertyModel<ContentBlockModel>(this, "contentblock"), toAdd, new IChoiceRenderer<ContentBlockModel>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(final ContentBlockModel object) {
                return providerRender.getDisplayValue(object.getProvider()) + " on " + renderer.getDisplayValue(object.getDocumentType());
            }

            @Override
            public String getIdValue(final ContentBlockModel object, final int index) {
                return String.valueOf(index);
            }
        });
        addToTypesListChoice.setOutputMarkupId(true);

        availableTypesListChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if (valid()) {
                    final String input = availableTypesListChoice.getInput();
                    final ContentBlockModel contentBlockModel = new ContentBlockModel(ContentBlocksPlugin.this.provider, ContentBlocksPlugin.this.selected, ContentBlocksPlugin.this.type, name, input);
                    if (addContentBlockToType(contentBlockModel)) {
                        toAdd.add(contentBlockModel);
                        target.add(addToTypesListChoice);
                        info(String.format("added provider: %s to document type %s", ContentBlocksPlugin.this.provider, renderer.getDisplayValue(input)));
                        target.add(feedbackPanel);
                    }
                } else {
                    error("Please fill in all the provider required fields before adding it to a document type");
                    target.add(feedbackPanel);
                }
            }
        });
        add(availableTypesListChoice);

        addToTypesListChoice.add(new AjaxEventBehavior("onchange") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                final ContentBlockModel contentBlockModel = addToTypesListChoice.getChoices().get(Integer.parseInt(addToTypesListChoice.getInput()));
                if (removeContentBlockFromType(contentBlockModel)) {
                    toAdd.remove(contentBlockModel);
                    target.add(addToTypesListChoice);
                    info(String.format("removed provider: %s to document type %s", contentBlockModel.getProvider(), renderer.getDisplayValue(contentBlockModel.getDocumentType())));
                    target.add(feedbackPanel);
                }

            }
        });
        add(addToTypesListChoice);
    }

    private boolean removeContentBlockFromType(final ContentBlockModel contentBlockModel) {
        final Session session = getContext().getSession();
        final String documentType = contentBlockModel.getDocumentType();
        final String myName = contentBlockModel.getName();
        try {
            Node docType;
            if (documentType.contains(":")) {
                docType = session.getNode("/hippo:namespaces/" + documentType.replace(':', '/'));
            } else {
                docType = session.getNode("/hippo:namespaces/system/" + documentType);
            }
            final String nodeTypePath = String.format("hipposysedit:nodetype/hipposysedit:nodetype/%s", myName);
            if (docType.hasNode(nodeTypePath)) {
                docType.getNode(nodeTypePath).remove();
            }
            final String templatePath = String.format("editor:templates/_default_/%s", myName);
            if (docType.hasNode(templatePath)) {
                docType.getNode(templatePath).remove();
            }
            if (!docType.hasNode(templatePath) && !docType.hasNode(templatePath)) {
                session.save();
                return true;
            } else {
                //TODO add error message
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    // TODO refactor this
    private boolean addContentBlockToType(final ContentBlockModel contentBlockModel) {
        final String documentType = contentBlockModel.getDocumentType();
        final Session session = getContext().getSession();
        InputStream in = null;

        try {
            Node docType;
            if (documentType.contains(":")) {
                docType = session.getNode("/hippo:namespaces/" + documentType.replace(':', '/'));
            } else {
                docType = session.getNode("/hippo:namespaces/system/" + documentType);
            }

            Node nodeType = null;
            if (docType.hasNode("hipposysedit:nodetype/hipposysedit:nodetype")) {
                nodeType = docType.getNode("hipposysedit:nodetype/hipposysedit:nodetype");
            }
            if (docType.hasNode("editor:templates/_default_/root")) {
                final Node ntemplate = docType.getNode("editor:templates/_default_");
                final Node root = docType.getNode("editor:templates/_default_/root");
                PluginType pluginType = null;
                if (root.hasProperty("plugin.class")) {
                    pluginType = PluginType.get(root.getProperty("plugin.class").getString());
                }
                if (pluginType != null) {
                    //Load template from source folder
                    /*Template template = cfg.getTemplate("nodetype.xml");
                    Template template2 = cfg.getTemplate("template.xml");*/
                    // Build the data-model
                    Map<String, Object> data = new HashMap<>();

                    data.put("name", contentBlockModel.getName());
                    data.put("path", new StringCodecFactory.UriEncoding().encode(contentBlockModel.getName()));
                    data.put("documenttype", documentType);
                    data.put("namespace", documentType.substring(0, documentType.indexOf(':')));
                    data.put("type", contentBlockModel.getType().getType());
                    data.put("provider", contentBlockModel.getProvider());

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

                    String parsed = TemplateUtils.injectTemplate("nodetype.xml", data, getClass());

                    in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));

                    ((HippoSession) session).importDereferencedXML(nodeType.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

                    parsed = TemplateUtils.injectTemplate("template.xml", data, getClass());
                    in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));

                    ((HippoSession) session).importDereferencedXML(ntemplate.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);
                    session.save();
                    return true;
                }
            }

        } catch (RepositoryException  | IOException e) {
            GlobalUtils.refreshSession(session, false);
            log.error("Error in content bocks plugin", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return false;
    }

    public List<ContentBlockModel> getContentBlocks() {
        List<ContentBlockModel> list = new ArrayList<>();
        final Session session = getContext().getSession();
        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            @SuppressWarnings("deprecation")
            final Query query = queryManager.createQuery("hippo:namespaces//element(*,frontend:plugin)[@plugin.class = 'org.onehippo.forge.contentblocks.ContentBlocksFieldPlugin']", Query.XPATH);
            final NodeIterator it = query.execute().getNodes();
            while (it.hasNext()) {
                final Node node = it.nextNode();
                ContentBlockModel model = new ContentBlockModel(node);
                list.add(model);
            }
        } catch (RepositoryException e) {
            log.error("repository exception while tying to pop", e);
        }
        return list;
    }

    private boolean valid() {
        if (StringUtils.isEmpty(provider) || StringUtils.isEmpty(name)) {
            return false;
        }
        return true;
    }

    @Override
    public ContentBlocksInstaller getInstaller() {
        return new ContentBlocksInstaller();
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

    public enum Type implements Serializable {
        LINKS("links"), DROPDOWN("dropdown");
        String type;

        private Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum PluginType {

        LISTVIEWPLUGIN("org.hippoecm.frontend.service.render.ListViewPlugin"), TWOCOLUMN("org.hippoecm.frontend.editor.layout.TwoColumn"), UNKNOWN("unknown");
        String clazz;

        PluginType(String clazz) {
            this.clazz = clazz;
        }

        public static PluginType get(String clazz) {
            for (PluginType a : PluginType.values()) {
                if (a.clazz.equals(clazz)) {
                    return a;
                }
            }
            return UNKNOWN;
        }

        public String getClazz() {
            return clazz;
        }

    }


    public enum WicketId {
        LEFT("${cluster.id}.left.item"), RIGHT("${cluster.id}.right.item"), DEFAULT("${cluster.id}.field");
        private String wicketId;

        WicketId(final String wicketId) {
            this.wicketId = wicketId;
        }

        public static WicketId get(String id) {
            for (WicketId a : WicketId.values()) {
                if (a.getWicketId().equals(id)) {
                    return a;
                }
            }
            return DEFAULT;
        }

        private String getWicketId() {
            return wicketId;
        }

    }

}
