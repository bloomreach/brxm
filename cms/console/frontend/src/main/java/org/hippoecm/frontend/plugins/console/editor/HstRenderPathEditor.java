/*
 *  Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.plugins.console.icons.FontAwesomeIcon;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstRenderPathEditor extends Panel {

    static final Logger log = LoggerFactory.getLogger(HstRenderPathEditor.class);

    protected static final String PROPERTY_HST_RENDERPATH = "hst:renderpath";
    protected static final String NODE_WEBFILES = "/webfiles";
    protected static final String WEBFILE_PREFIX = "webfile:/";


    public HstRenderPathEditor(final String id, final IModel<String> valueModel) {
        super(id);
        setOutputMarkupId(true);

        final String propertyValue = valueModel.getObject();

        // validation icon
        Label validationIcon = new Label("validation-icon", StringUtils.EMPTY);
        add(validationIcon);
        determineIcon(validationIcon, propertyValue);

        // input field
        final TextAreaWidget editor = new TextAreaWidget("reference-edit", valueModel);
        editor.setRows("1");
        add(editor);
    }

    private void determineIcon(final Label validationIcon, final String propertyValue) {
        if (StringUtils.isBlank(propertyValue) || !propertyValue.startsWith(WEBFILE_PREFIX)) {
            validationIcon.setVisible(false);
            return;
        }

        // iterate over bundles to find the template mentioned
        ArrayList<String> webFilePaths = new ArrayList<>();
        try {
            final Session jcrSession = UserSession.get().getJcrSession();
            for (String bundlePath : getWebfileBundlePaths()) {
                final String webfilePath = bundlePath + '/' + propertyValue.substring(WEBFILE_PREFIX.length());
                if (jcrSession.itemExists(webfilePath)) {
                    webFilePaths.add(webfilePath);
                }
            }
        } catch (RepositoryException e) {
            log.error("Cannot retrieve webfiles node.", e);
            validationIcon.setVisible(false);
            return;
        }

        switch (webFilePaths.size()) {
            case 0:
                validationIcon.add(ClassAttribute.append(FontAwesomeIcon.EXCLAMATION.cssClass() + " check-false"));
                validationIcon.add(TitleAttribute.append("Web File not found at this path."));
                break;
            case 1:
                validationIcon.add(ClassAttribute.append(FontAwesomeIcon.CHECK.cssClass() + " check-ok"));
                validationIcon.add(TitleAttribute.append("Web File Found at path " + webFilePaths.get(0)));
                break;
            default:
                validationIcon.add(ClassAttribute.append(FontAwesomeIcon.CHECK.cssClass() + " check-ok"));
                validationIcon.add(TitleAttribute.append("Web File Found at multiple paths: "
                        + StringUtils.join(webFilePaths, ", ")));
        }
    }

    private ArrayList<String> getWebfileBundlePaths() {
        final ArrayList<String> webfileBundlePaths = new ArrayList<>();
        try {
            final Node webfilesNode = UserSession.get().getJcrSession().getNode(NODE_WEBFILES);
            final NodeIterator nodeIterator = webfilesNode.getNodes();
            while (nodeIterator.hasNext()) {
                webfileBundlePaths.add(nodeIterator.nextNode().getPath());
            }
        } catch (PathNotFoundException e) {
            log.warn(String.format("Webfiles not found at path %s. " +
                    "Cannot check hst:renderpath properties that reference web files.", NODE_WEBFILES));
        } catch (RepositoryException e) {
            log.error("Cannot retrieve webfiles node.", e);
        }
        return webfileBundlePaths;
    }

}
