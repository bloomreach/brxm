/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.editor;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugins.console.behavior.OriginTitleBehavior;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.definition.ContentDefinition;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.ConfigurationNode;
import org.onehippo.cm.model.tree.ConfigurationProperty;
import org.onehippo.cm.model.tree.ModelItem;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.joining;
import static org.onehippo.cm.model.util.ConfigurationModelUtils.getCategoryForNode;
import static org.onehippo.cm.model.util.ConfigurationModelUtils.getCategoryForProperty;

public class PropertiesEditor extends DataView<Property> {

    static final Logger log = LoggerFactory.getLogger(PropertiesEditor.class);

    private final String namespacePrefix;
    // the (transient, not serializable) HCM ConfigurationService, which is repo-static, but not the model, which can be updated
    private transient ConfigurationService cfgService;

    PropertiesEditor(final String id, final IDataProvider<Property> model) {
        super(id, model);

        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());

        final String namespace = ((NodeEditor.NamespacePropertiesProvider) model).getNamespace();

        if (namespace.equals(NodeEditor.NONE_LABEL)) {
            namespacePrefix = StringUtils.EMPTY;
        } else {
            namespacePrefix = namespace + ":";
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        cfgService = null;
    }

    private ConfigurationService getConfigurationService() {
        if (cfgService == null) {
            cfgService = HippoServiceRegistry.getService(ConfigurationService.class);
        }
        return cfgService;
    }

    @Override
    protected void populateItem(final Item item) {
        final JcrPropertyModel model = (JcrPropertyModel) item.getModel();

        try {
            final AjaxLink deleteLink = deleteLink("delete", model);
            item.add(deleteLink);
            deleteLink.setVisible(!model.getProperty().getDefinition().isProtected());

            final JcrName propName = new JcrName(model.getProperty().getName());
            item.add(new Label("namespace", namespacePrefix));
            item.add(new Label("name", propName.getName()));

            item.add(new Label("type", PropertyType.nameFromValue(model.getProperty().getType())));

            final WebMarkupContainer valuesContainer = new WebMarkupContainer("values-container");
            valuesContainer.setOutputMarkupId(true);
            item.add(valuesContainer);

            final PropertyValueEditor editor = createPropertyValueEditor("values", model);
            valuesContainer.add(editor);

            final AjaxLink addLink = addLink("add", model, valuesContainer, editor);
            addLink.add(TitleAttribute.set(getString("property.value.add")));
            item.add(addLink);

            final PropertyDefinition definition = model.getProperty().getDefinition();
            addLink.setVisible(definition.isMultiple() && !definition.isProtected());

            // HCM config-tracing info
            final ConfigurationModel cfgModel = getConfigurationService().getRuntimeConfigurationModel();
            final String origin = getPropertyOrigin(model.getProperty().getPath(), cfgModel);
            item.add(new Label("origin", "").add(new OriginTitleBehavior(Model.of(origin))));
        } catch (final RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    static String getPropertyOrigin(final String propertyPath, final ConfigurationModel cfgModel) {
        String nodePath = StringUtils.substringBeforeLast(propertyPath, "/");
        if (nodePath.equals("")) {
            nodePath = "/";
        }

        // runtime nodes only have runtime properties
        final ConfigurationItemCategory nodeCat = getCategoryForNode(nodePath, cfgModel);
        if (nodeCat.equals(ConfigurationItemCategory.SYSTEM)) {
            return "";
        }

        final String nodeOrigin = getNodeOrigin(nodePath, cfgModel);

        final ConfigurationItemCategory propCat = getCategoryForProperty(propertyPath, cfgModel);
        final JcrPath propertyJcrPath = JcrPaths.getPath(propertyPath);
        final ConfigurationProperty cfgProperty = cfgModel.resolveProperty(propertyJcrPath);
        final String origin;
        if ((propCat.equals(ConfigurationItemCategory.CONFIG) || propCat.equals(ConfigurationItemCategory.SYSTEM))
                && cfgProperty != null) {
            origin = cfgProperty.getDefinitions().stream().map(ModelItem::getOrigin).collect(joining("\n"));

            // TODO: mark system-with-initial-value in a special way?
        }
        else if (propCat.equals(ConfigurationItemCategory.CONTENT)) {
            final ContentDefinition def = cfgModel.getNearestContentDefinition(propertyJcrPath);
            origin = (def==null)
                    ? "<content>"
                    : def.getOrigin();
        }
        else if (propCat.equals(ConfigurationItemCategory.SYSTEM)) {
            origin = "<system>";
        }
        else {
            origin = "<config>";
        }
        return nodeOrigin.equals(origin)
                ? ""
                // double \n so we get nice spacing in the title tool-tip pop-up
                : origin.replace("\n", "\n\n");
    }

    static String getNodeOrigin(final String nodePath, final ConfigurationModel cfgModel) {
        final ConfigurationItemCategory nodeCat = getCategoryForNode(nodePath, cfgModel);
        final String nodeOrigin;
        final JcrPath jcrPath = JcrPaths.getPath(nodePath);
        if (nodeCat.equals(ConfigurationItemCategory.CONFIG)) {
            final ConfigurationNode cfgNode = cfgModel.resolveNode(jcrPath);
            nodeOrigin = (cfgNode==null)
                    ? "<config>"
                    : cfgNode.getDefinitions().stream().map(ModelItem::getOrigin).collect(joining("\n"));
        }
        else if (nodeCat.equals(ConfigurationItemCategory.CONTENT)) {
            final ContentDefinition def = cfgModel.getNearestContentDefinition(jcrPath);
            nodeOrigin = (def==null)
                    ? "<content>"
                    : def.getOrigin();
        }
        else {
            nodeOrigin = "<system>";
        }
        return nodeOrigin;
    }

    /**
     * Creates {@link PropertyValueEditor} for a property. If name equals 'hst:script' and parent node is of type
     * 'hst:template' a FreemarkerCodeMirrorPropertyValueEditor is returned instead.
     *
     * @throws RepositoryException
     */
    private PropertyValueEditor createPropertyValueEditor(final String id, final JcrPropertyModel model) throws RepositoryException {

        // NOTES:
        // - Creates custom propertyValueEditor for freemarker template source.
        // - For now, it's hard-coded only for Freemarker templates, but maybe someday we can improve it to read
        //   from plugin configurations if we want to support more custom propertyEditors (e.g, css, js, etc.).

        final Property prop = model.getProperty();

        if ("hst:script".equals(prop.getName()) && prop.getParent().isNodeType("hst:template")) {
            return new FreemarkerCodeMirrorPropertyValueEditor(id, model);
        }

        return new PropertyValueEditor(id, model);
    }

    // privates
    private AjaxLink deleteLink(final String id, final JcrPropertyModel model) {
        final AjaxLink deleteLink = new AjaxLink<Property>(id, model) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    final Property prop = model.getProperty();
                    prop.remove();
                } catch (final RepositoryException e) {
                    log.error(e.getMessage());
                }

                final NodeEditor editor = findParent(NodeEditor.class);
                target.add(editor);
            }
        };

        deleteLink.add(ClassAttribute.set("property-remove"));
        deleteLink.add(TitleAttribute.set(getString("property.remove")));

        return deleteLink;
    }

    private AjaxLink addLink(final String id, final JcrPropertyModel model, final WebMarkupContainer component,
                             final PropertyValueEditor editor) {

        return new AjaxLink<Property>(id, model) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final Property prop = model.getProperty();
                final Value[] newValues;
                try {
                    final Value[] oldValues = prop.getValues();
                    newValues = new Value[oldValues.length + 1];
                    System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                    newValues[newValues.length - 1] = createDefaultValue(prop.getType());
                    prop.setValue(newValues);

                    editor.setFocusOnLastItem(true);
                } catch (final RepositoryException e) {
                    log.error(e.getMessage());
                    return;
                }
                target.add(component);
            }
        };
    }

    private Value createDefaultValue(final int valueType) throws RepositoryException {
        final ValueFactory valueFactory = UserSession.get().getJcrSession().getValueFactory();
        switch (valueType) {
            case PropertyType.BOOLEAN : return valueFactory.createValue(false);
            case PropertyType.DATE : return valueFactory.createValue(Calendar.getInstance());
            case PropertyType.DECIMAL : return valueFactory.createValue(BigDecimal.valueOf(0d));
            case PropertyType.DOUBLE : return valueFactory.createValue(0d);
            case PropertyType.LONG : return valueFactory.createValue(0L);
            case PropertyType.NAME : return valueFactory.createValue("jcr:name", PropertyType.NAME);
            case PropertyType.PATH : return valueFactory.createValue("/", PropertyType.PATH);
            case PropertyType.URI : return valueFactory.createValue("http://www.onehippo.org/", PropertyType.URI);
            case PropertyType.REFERENCE : return valueFactory.createValue("cafebabe-cafe-babe-cafe-babecafebabe", PropertyType.REFERENCE);
            case PropertyType.WEAKREFERENCE : return valueFactory.createValue("cafebabe-cafe-babe-cafe-babecafebabe", PropertyType.REFERENCE);
            default : return valueFactory.createValue("");
        }
    }

}
