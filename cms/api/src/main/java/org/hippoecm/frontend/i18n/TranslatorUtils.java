/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.i18n;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.FrontendNodeType;

public class TranslatorUtils {

    public static final String EDITOR_TEMPLATES_NODETYPE = "editor:templates";
    public static final String EDITOR_TEMPLATESET_NODETYPE = "editor:templateset";
    public static final String CAPTION_PROPERTY = "caption";

    private TranslatorUtils() {}

    /**
     * Get the first node of type 'frontend:plugincluster' under the 'editor:templates' node
     *
     * @param typeNode a node of type 'editor:templates'
     * @return the template node or thrown exceptions
     * @throws javax.jcr.RepositoryException
     * @throws org.hippoecm.frontend.i18n.TranslatorException if either no child node of type 'frontend:plugincluster'
     * is found, <code>typeNode</code> is null or not the type 'editor:templates'.
     */
    public static Node getTemplateNode(final Node typeNode) throws TranslatorException {
        try {
            if (typeNode == null || !typeNode.hasNode(EDITOR_TEMPLATES_NODETYPE)) {
                throw new TranslatorException("Invalid node of type " + EDITOR_TEMPLATES_NODETYPE);
            }
            final Node templateSetNode = typeNode.getNode(EDITOR_TEMPLATES_NODETYPE);
            if (templateSetNode == null || !templateSetNode.isNodeType(EDITOR_TEMPLATESET_NODETYPE)) {
                throw new TranslatorException("Invalid node of type " + EDITOR_TEMPLATESET_NODETYPE);
            }

            NodeIterator pluginClusterNodes = templateSetNode.getNodes();
            while (pluginClusterNodes.hasNext()) {
                final Node templateNode = pluginClusterNodes.nextNode();
                if (templateNode != null && templateNode.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                    return templateNode;
                }
            }
            throw new TranslatorException("Cannot find child node of type " + FrontendNodeType.NT_PLUGINCLUSTER);
        } catch (RepositoryException e) {
            throw new TranslatorException("Cannot find template node", e);
        }
    }

    /**
     * Create a model containing the translated message for the given exception and its parameters. The message will be
     * loaded from the resource properties files of either component or exception class with following syntax:
     * <pre>
     * {@code
     *  exception,type\=${exception-class-path},message\=${exception-message}=<your-localized-message>
     * }
     * </pre>
     *
     * @param component  the component having translated resource files
     * @param t  the throwable exception
     * @param parameters parameters used in the message template storing in resource files
     * @return  A model for the translated exception message.
     */
    public static IModel<String> getExceptionTranslation(final Component component,
                                                         final Throwable t, final Object... parameters) {
        HashMap<String, String> details = new HashMap<>();

        return new StringResourceModel(createKey(t, details), component)
                .setModel(Model.of(details))
                .setDefaultValue(t.getLocalizedMessage())
                .setParameters(parameters);
    }

    /**
     * Create a model containing the translated message for the given exception and its parameters. The message will be
     * loaded from a class's resource properties files with following syntax:
     * <pre>
     * {@code
     *  exception,type\=${exception-class-path}
     * }
     * </pre>
     *
     * This method was created due to CMS-9656, where the GalleryUploadPanel failed to retrieve messages from its
     * own resource bundle due to an intricate inheritance structure. Using the clazz instead of a component
     * circumvents the inheritance structure as far as necessary.
     *
     * @param clazz  the class having translated resource files
     * @param t  the throwable exception
     * @param parameters parameters used in the message template storing in resource files
     * @return  A model for the translated exception message.
     */
    public static IModel<String> getExceptionTranslation(final Class clazz, final Throwable t,
                                                         final Object... parameters) {
        final String key = "exception,type=${type},class=${clazz}";
        HashMap<String, String> details = new HashMap<>();
        details.put("type", t.getClass().getName());
        details.put("clazz", clazz.getName());

        return new StringResourceModel(key)
                .setModel(Model.of(details))
                .setDefaultValue(t.getLocalizedMessage())
                .setParameters(parameters);
    }

    /**
     * Create the resource key and assemble the details.
     *
     * @param t        exception for which to create the key
     * @param details  map to hold the to-be-interpolated details
     * @return         resource key
     */
    private static String createKey(final Throwable t, final Map<String, String> details) {
        String key = "exception,type=${type},message=${message}";
        details.put("type", t.getClass().getName());
        details.put("message", t.getMessage());
        StackTraceElement[] elements = t.getStackTrace();
        if (elements.length > 0) {
            StackTraceElement top = elements[0];
            details.put("clazz", top.getClassName());
            key += ",class=${clazz}";
        }
        return key;
    }
}
