/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standards.perspective;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>Add this {@link Behavior} to a {@link Perspective} to add an iframe that communicates
 * with the cms using the navigation application communication library.</p>
 *
 * <p>This behavior:
 * <li>adds an iframe element with {@link #iFrameElementId} and {@link #iFrameElementSrc}</li>
 * <li>adds javascript to header ( after the navapp-communication script has loaded ) that:</li>
 * <ul>
 * <li>Connects to the iframe</li>
 * <li>Provides an implementation of the Parent API</li>
 * </ul>
 * </p>
 *
 * <p>The application running inside the iframe is responsible for connecting to the cms iframe.</p>
 */
public class CmsSubAppIFrameCommunicationBehavior extends Behavior {

    private static final Logger log = LoggerFactory.getLogger(CmsSubAppIFrameCommunicationBehavior.class);
    private static final String CMS_SUBAPP_IFRAME_COMMUNICATION_JS = "cms-subapp-iframe-communication.js";
    private static final String I_FRAME_ELEMENT_ID_KEY = "iFrameElementId";
    private final String iFrameElementId;
    private final String iFrameElementSrc;

    /**
     * @param iFrameElementId Non null id attribute of the iframe element ( should not contain spaces )
     * @param iFrameElementSrc Non null src attribute of the iframe element
     */
    public CmsSubAppIFrameCommunicationBehavior(final String iFrameElementId, final String iFrameElementSrc) {
        log.info("Instantiate iFrameElementId:{}, iFrameElementSrc:{}", iFrameElementId, iFrameElementSrc);
        Validate.notNull(iFrameElementId);
        Validate.isTrue(!iFrameElementId.contains(" "));
        this.iFrameElementId = iFrameElementId;
        this.iFrameElementSrc = iFrameElementSrc;
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        if (component instanceof Perspective) {
            Perspective perspective = (Perspective) component;
            log.info("Bind perspective:{}, add iframe element:{id:{},src{}}",perspective.getClass().getName()
                    , this.iFrameElementId, this.iFrameElementSrc);
            final WebComponent iframe = new WebComponent("iframe") {

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("src", CmsSubAppIFrameCommunicationBehavior.this.iFrameElementSrc);
                    tag.put("class", CmsSubAppIFrameCommunicationBehavior.this.iFrameElementId);
                }
            };
            iframe.setMarkupId(CmsSubAppIFrameCommunicationBehavior.this.iFrameElementId);
            perspective.add(iframe);
        }
    }

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        log.info("Add script:{}",CMS_SUBAPP_IFRAME_COMMUNICATION_JS);
        super.renderHead(component, response);
        final CharSequence javaScript = JavaScriptHeaderItem
                .forScript(createScript(), CMS_SUBAPP_IFRAME_COMMUNICATION_JS).getJavaScript();

        response.render(OnDomReadyHeaderItem.forScript(javaScript));
    }

    private String createScript() {

        final Map<String, String> variables = new HashMap<>();
        log.info("Add key:{},value:{} as parameter to script", I_FRAME_ELEMENT_ID_KEY, this.iFrameElementId);
        variables.put(I_FRAME_ELEMENT_ID_KEY, this.iFrameElementId);

        try (final PackageTextTemplate javaScript = new PackageTextTemplate(CmsSubAppIFrameCommunicationBehavior.class
                , CMS_SUBAPP_IFRAME_COMMUNICATION_JS)) {
            return javaScript.asString(variables);
        } catch (IOException e) {
            log.error("Failed to create script for resource {}, returning empty string instead."
                    , CMS_SUBAPP_IFRAME_COMMUNICATION_JS, e);
            return "";
        }
    }

}
