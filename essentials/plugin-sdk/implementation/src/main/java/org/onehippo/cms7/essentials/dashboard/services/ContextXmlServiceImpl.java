/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Singleton;

import org.dom4j.Document;
import org.dom4j.Element;
import org.onehippo.cms7.essentials.dashboard.service.ContextXmlService;
import org.onehippo.cms7.essentials.dashboard.utils.Dom4JUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.springframework.stereotype.Service;

@Service
@Singleton
public class ContextXmlServiceImpl implements ContextXmlService {

    @Override
    public boolean addResource(final String name, final Map<String, String> attributes) {
        return addContextElement("Resource", name, attributes);
    }

    @Override
    public boolean addEnvironment(final String name, final Map<String, String> attributes) {
        return addContextElement("Environment", name, attributes);
    }

    private boolean addContextElement(final String elementType, final String name, final Map<String, String> attributes) {
        return Dom4JUtils.update(ProjectUtils.getContextXml(), doc -> {
            Element element = contextElementFor(doc, elementType, name);
            if (element == null) {
                element = createContextElement(doc, elementType, name);
            } else {
                // delete all existing attributes (if any)
                element.setAttributes(new ArrayList<>());
                element.addAttribute("name", name);
            }
            if (attributes != null) {
                for (String attributeName : attributes.keySet()) {
                    element.addAttribute(attributeName, attributes.get(attributeName));
                }
            }
        });
    }

    private Element contextElementFor(final Document doc, final String elementType, final String name) {
        final String selector = String.format("/Context/%s[@name='%s']", elementType, name);
        return (Element) doc.getRootElement().selectSingleNode(selector);
    }

    private Element createContextElement(final Document doc, final String elementType, final String name) {
        Element context = (Element) doc.getRootElement().selectSingleNode("/Context");
        if (context == null) {
            context = Dom4JUtils.addIndentedElement(doc.getRootElement(), "Context", null);
        }
        return Dom4JUtils.addIndentedSameNameSibling(context, elementType, null)
                .addAttribute("name", name);
    }
}
