package org.onehippo.cms7.essentials.dashboard.utils.wicket;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.nodetypes.NodeTypeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ripped from org.hippoecm.frontend.i18n.types.TypeTranslator to make it work without a wicket/jcr Usersession.
 * The Essentials dashboard does not support Session retrieval through this way.
 *
 * @version "$Id$"
 */
public class TypeTranslator extends NodeTypeModelWrapper {

    final static Logger log = LoggerFactory.getLogger(TypeTranslator.class);

    private static final long serialVersionUID = 1L;

    private TypeNameModel name;
    private transient boolean attached = false;
    private transient Node nodeModel;
    private transient PluginContext context;

    public TypeTranslator(PluginContext context, JcrNodeTypeModel nodeTypeModel) {
        super(nodeTypeModel);
        name = new TypeNameModel();
        this.context = context;
    }

    public IModel<String> getTypeName() {
        return name;
    }

    public IModel<String> getValueName(String property, IModel<String> value) throws RepositoryException {
        attach();
        return new PropertyValueModel(property, value);
    }

    public IModel<String> getPropertyName(String compoundName) throws RepositoryException {
        attach();
        return new PropertyModel(compoundName);
    }

    @Override
    public void detach() {
        if (attached) {
            super.detach();
            name.onDetachTranslator();
            nodeModel = null;
            attached = false;
        }
    }

    // internals

    private void attach() throws RepositoryException {
        if (!attached) {
            String type = getNodeTypeModel().getType();
            if (type.contains(":")) {
                nodeModel = context.getSession().getNode("/hippo:namespaces/" + type.replace(':', '/'));
            } else {
                nodeModel = context.getSession().getNode("/hippo:namespaces/system/" + type);
            }
            attached = true;
        }
    }

    private Node getNodeModel() throws RepositoryException {
        attach();
        return nodeModel;
    }

    private class TypeNameModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        @Override
        protected String load() {
            String myName = getNodeTypeModel().getType();
            try {
                Node node = getNodeModel();
                if (node != null) {
                    myName = NodeNameCodec.decode(node.getName());
                    if (node.isNodeType("hippo:translated")) {
                        Locale locale = org.apache.wicket.Session.get().getLocale();
                        NodeIterator nodes = node.getNodes("hippo:translation");
                        while (nodes.hasNext()) {
                            Node child = nodes.nextNode();
                            if (child.isNodeType("hippo:translation") && !child.hasProperty("hippo:property")) {
                                String language = child.getProperty("hippo:language").getString();
                                if (locale.getLanguage().equals(language)) {
                                    return child.getProperty("hippo:message").getString();
                                }
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }

            return myName;
        }

        void onDetachTranslator() {
            super.detach();
        }

        @Override
        public void detach() {
            TypeTranslator.this.detach();
        }
    }

    class PropertyValueModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        private String property;
        private IModel<String> value;

        PropertyValueModel(String property, IModel<String> value) {
            this.property = property;
            this.value = value;
        }

        @Override
        protected String load() {
            IModel<String> myName = value;
            try {
                Node node = getNodeModel();
                if (node != null) {
                    if (node.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                        Locale locale = org.apache.wicket.Session.get().getLocale();
                        NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                        while (nodes.hasNext()) {
                            Node child = nodes.nextNode();
                            if (child.isNodeType(HippoNodeType.NT_TRANSLATION) && child.hasProperty(HippoNodeType.HIPPO_PROPERTY)
                                    && child.hasProperty(HippoNodeType.HIPPO_VALUE)) {
                                if (child.getProperty(HippoNodeType.HIPPO_PROPERTY).getString().equals(property)
                                        && child.getProperty(HippoNodeType.HIPPO_VALUE).getString().equals(myName.getObject())) {
                                    String language = child.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return myName.getObject();
        }

        @Override
        public void detach() {
            super.detach();
            value.detach();
            TypeTranslator.this.detach();
        }

    }

    class PropertyModel extends LoadableDetachableModel<String> {
        private static final long serialVersionUID = 1L;

        private String propertyName;

        PropertyModel(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        protected String load() {
            Node myModeModel = null;
            try {
                myModeModel = getNodeModel();
            } catch (RepositoryException e) {
                log.error("", e);
            }
            if (myModeModel != null) {
                try {
                    if (myModeModel.isNodeType(HippoNodeType.NT_TRANSLATED)) {
                        Locale locale = org.apache.wicket.Session.get().getLocale();
                        NodeIterator nodes = myModeModel.getNodes(HippoNodeType.HIPPO_TRANSLATION);
                        while (nodes.hasNext()) {
                            Node child = nodes.nextNode();
                            if (child.isNodeType(HippoNodeType.NT_TRANSLATION) && child.hasProperty(HippoNodeType.HIPPO_PROPERTY)) {
                                if (child.getProperty(HippoNodeType.HIPPO_PROPERTY).getString().equals(propertyName)) {
                                    String language = child.getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
                                    if (locale.getLanguage().equals(language)) {
                                        return child.getProperty(HippoNodeType.HIPPO_MESSAGE).getString();
                                    }
                                }
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
            int colonIndex = propertyName.indexOf(':');
            if (colonIndex != -1 && colonIndex + 1 < propertyName.length()) {
                return propertyName.substring(colonIndex + 1);
            } else {
                return propertyName;
            }
        }

        @Override
        public void detach() {
            super.detach();
            TypeTranslator.this.detach();
        }
    }

}
