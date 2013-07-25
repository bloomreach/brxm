/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.header;

import java.util.Map;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YuiNamespace;

/**
 * This interface is used to render the following header elements: YUI-modules, javascript sources, css stylesheets,
 * on-dom-load/on-win-load scripts, {@link DynamicTextTemplate}s and {@link FinalTextTemplate}s.  
 * 
 * Implementations are responsible for loading YUI-modules and filtering static resources from subsequent requests, to 
 * minimize the response footprint.  
 */
public interface IYuiContext extends IHeaderContributor {

    /**
     * Load YUI module from the {@link org.onehippo.yui.YahooNamespace} context
     * 
     * @param module 
     *            YUI module name
     */
    void addModule(String module);

    /**
     * Load YUI module from the specified {@link org.onehippo.yui.YuiNamespace} context
     * 
     * @param module 
     *            YUI module name
     * @param namespace 
     *            {@link org.onehippo.yui.YuiNamespace} to use as context for module
     */
    void addModule(YuiNamespace namespace, String module);

    /**
     * Add a static {@link TextTemplate} to the response. The model provided by the parameters <code>Map</code> is final.
     *
     * @param clazz
     *            Class that acts as context
     * @param filename
     *            Name of file relative to provided class
     * @param parameters
     *            Parameters that will be interpolated with the {@link TextTemplate}
     */
    void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters);

    /**
     * Add a static template to the response.
     * 
     * @param template
     *            {@link TextTemplate} that will be added to the response.
     */
    @Deprecated
    void addTemplate(FinalTextTemplate template);

    /**
     * Add a {@link DynamicTextTemplate} to the response. The model will be refreshed upon every request.
     * 
     * @param template
     *            {@link DynamicTextTemplate} that will be added to the response.
     */
    @Deprecated
    void addTemplate(DynamicTextTemplate template);

    /**
     * Add a generic template to the header response. This method should replace the other add*Template methods.
     * @param template
     */
    void addTemplate(IHeaderContributor template);

    /**
     * Add static javascript <code>String</code> that will be executed on the browsers' window-load event
     * 
     * @param onload 
     *            <code>String</code> of javascript code that will be executed on the client.  
     */
    void addOnWinLoad(String onload);

    /**
     * Add dynamic javascript that will be executed on the browsers' window-load event.
     * 
     * @param model 
     *            {@link IModel} which returns javascript code that will be executed on the client.
     */
    void addOnWinLoad(IModel<String> model);

    /**
     * Add static javascript <code>String</code> that will be executed on the browsers' dom-ready event
     * 
     * @param onload 
     *            <code>String</code> of javascript code that will be executed on the client.  
     */
    void addOnDomLoad(String onload);

    /**
     * Add dynamic javascript that will be executed on the browsers' dom-ready event.
     * 
     * @param model 
     *            {@link IModel} which returns javascript code that will be executed on the client.
     */
    void addOnDomLoad(IModel<String> model);
    
    /**
     * Helper method for adding a javascript reference
     * 
     * @param reference
     *            {@link ResourceReference} that should be added to the head
     */
    void addJavascriptReference(ResourceReference reference);

    /**
     * Helper method for adding css reference
     *
     * @param reference
     *            {@link ResourceReference} that should be added to the head
     */
    void addCssReference(ResourceReference reference);

    /**
     * Return a header item for the modules and references
     */
    HeaderItem getHeaderItem();

}
