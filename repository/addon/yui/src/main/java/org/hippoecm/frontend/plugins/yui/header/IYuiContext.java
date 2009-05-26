/*
 *  Copyright 2008 Hippo.
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

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YuiNamespace;

public interface IYuiContext extends IHeaderContributor {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    /**
     * Add a YUI module from the YAHOO namespace
     * @param module YUI module name
     */
    void addModule(String module);

    /**
     * Add a YUI module from the specified namespace
     * @param ns Namespace to load module from
     * @param module Module name
     */
    void addModule(YuiNamespace ns, String module);

    /**
     * Add a static template to the response. The model provided by the parameters map is final.
     * @param clazz
     * @param filename
     * @param parameters
     */
    void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters);

    /**
     *
     * @param template
     */
    void addTemplate(FinalTextTemplate template);

    /**
     * Add a dynamic template to the response. The model will be refreshed upon every request.
     * @param template
     */
    void addTemplate(DynamicTextTemplate template);

    /**
     * Add javascript that get's executed on pageLoad
     * @param string String of javascript code that is evaluated on the client
     */
    void addOnWinLoad(String string);

    void addOnDomLoad(String string);

    void addJavascriptReference(ResourceReference reference);

    void addCssReference(ResourceReference reference);

}
