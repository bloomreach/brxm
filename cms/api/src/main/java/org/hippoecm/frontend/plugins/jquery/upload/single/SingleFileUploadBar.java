/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.single;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings;
import org.hippoecm.frontend.widgets.UpdateFeedbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create an unique instance of jquery-file-upload widget. It does not work for multiple instances jquery-file-upload
 */
public class SingleFileUploadBar extends Panel {
    private static final long serialVersionUID = 1L;
    static private final Logger log = LoggerFactory.getLogger(SingleFileUploadBar.class);

//    private static final CssResourceReference CSS = new CssResourceReference(SingleFileUploadBar.class, "SingleFileUploadBar.css");;

    private static final String JQUERY_FILEUPLOAD_SINGLE_JS = "jquery.fileupload-single.js";
    private static final String CONFIG_JS = "widget-config.js";

    public SingleFileUploadBar(final String id, final FileUploadWidgetSettings settings) {
        super(id);
        setOutputMarkupId(true);

        AbstractDefaultAjaxBehavior refreshAjaxBehavior;
        add(refreshAjaxBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(final AjaxRequestTarget target) {
                log.debug("Received an ajax callback refreshing page");
                target.add(SingleFileUploadBar.this.getPage());
                // refresh feedback panel in EditPerspective if needed
                send(SingleFileUploadBar.this, Broadcast.BUBBLE, new UpdateFeedbackInfo(target));
            }
        });

        final AbstractDefaultAjaxBehavior onChangeAjaxBehavior;
        add(onChangeAjaxBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(final AjaxRequestTarget target) {
                onChange(target);
            }
        });

        add(new FileUploadBehavior(settings) {

            @Override
            protected void renderScripts(final IHeaderResponse response) {
                super.renderScripts(response);
                response.render(JavaScriptHeaderItem.forReference(
                        new JavaScriptResourceReference(SingleFileUploadBar.class, JQUERY_FILEUPLOAD_SINGLE_JS)));
            }

            @Override
            protected Map<String, Object> configureParameters(final Component component) {
                final Map<String, Object> variables = super.configureParameters(component);

                //the script to refresh page after sending file has been done
                variables.put("callbackRefreshScript", refreshAjaxBehavior.getCallbackScript());
                variables.put("callbackFileOnChangeScript", onChangeAjaxBehavior.getCallbackScript());

                onConfigureParameters(variables);
                return variables;
            }

            @Override
            protected void renderWidgetConfig(final IHeaderResponse response, final Map<String, Object> variables) {
                PackageTextTemplate jsTmpl = new PackageTextTemplate(SingleFileUploadBar.class, CONFIG_JS);
                String s = jsTmpl.asString(variables);
                // call the configuration after all DOM elements are loaded
                response.render(OnDomReadyHeaderItem.forScript(s));
            }
        });
    }

    /**
     * This event is called when selecting a new file
     * @param target
     */
    protected void onChange(final AjaxRequestTarget target) {
    }

    /**
     * Override this method to insert more variables to the widget configuration
     *
     * @param variables
     */
    protected void onConfigureParameters(final Map<String, Object> variables) {
    }
}
