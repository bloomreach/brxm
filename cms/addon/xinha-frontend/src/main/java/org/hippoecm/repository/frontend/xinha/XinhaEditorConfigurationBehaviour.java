/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.frontend.xinha;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;

public class XinhaEditorConfigurationBehaviour extends AbstractHeaderContributor
{
    private static final long serialVersionUID = 1L;

    private Set configurations;

    public XinhaEditorConfigurationBehaviour() {
        configurations = new HashSet();
    }

    public void addConfiguration(XinhaEditorConf o) {
        configurations.add(o);
    }

    public final IHeaderContributor[] getHeaderContributors() {
        return new IHeaderContributor[] {

            new IHeaderContributor() {
                private static final long serialVersionUID = 1L;
                public void renderHead(IHeaderResponse response) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("_editor_url = 'http://localhost:8082/xinha/';\n");
                    sb.append("_editor_lang = 'en';\n");
                    response.renderJavascript(sb, null);
                }
            },

            //HeaderContributor.forJavaScript("/xinha/XinhaCore.js"),
            HeaderContributor.forJavaScript("http://localhost:8082/xinha/XinhaCore.js"),

            new IHeaderContributor() {
                private static final long serialVersionUID = 1L;
                public void renderHead(IHeaderResponse response) {
                    StringBuffer sb = new StringBuffer();
                    Set plugins = new HashSet();
                    for(Iterator iter = configurations.iterator(); iter.hasNext(); ) {
                        XinhaEditorConf conf = (XinhaEditorConf) iter.next();
                        String[] plugin = conf.getPlugins();
                        for(int i=0; i<plugin.length-1; i++)
                            plugins.add(plugin[i]);
                    }

                    sb.append("xinha_editors = null;\n");
                    sb.append("xinha_init    = null;\n");
                    sb.append("xinha_config  = null;\n");
                    sb.append("xinha_plugins = null;\n");
                    sb.append("xinha_init = xinha_init ? xinha_init : function()\n");
                    sb.append("{\n");
                    sb.append("  xinha_editors = xinha_editors ? xinha_editors :\n");
                    sb.append("  [\n");
                    for(Iterator iter = configurations.iterator(); iter.hasNext(); ) {
                        sb.append("    '");
                        sb.append(((XinhaEditorConf)iter.next()).getName());
                        sb.append("'");
                        if(iter.hasNext())
                            sb.append(",");
                        sb.append("\n");
                    }
                    sb.append("  ];\n");
                    sb.append("  xinha_plugins = xinha_plugins ? xinha_plugins :\n");
                    sb.append("  [\n");
                    for(Iterator iter = plugins.iterator(); iter.hasNext(); ) {
                        sb.append("    '");
                        sb.append((String)iter.next());
                        sb.append("'");
                        if(iter.hasNext())
                            sb.append(",");
                        sb.append("\n");
                    }
                    sb.append("  ];\n");
                    sb.append("  if(!Xinha.loadPlugins(xinha_plugins, xinha_init)) return;\n");
                    sb.append("  xinha_config = xinha_config ? xinha_config() : new Xinha.Config();\n");
                    sb.append("  xinha_config.pageStyleSheets = [ _editor_url + \"examples/full_example.css\" ];\n");
                    sb.append("  xinha_editors   = Xinha.makeEditors(xinha_editors, xinha_config, xinha_plugins);\n");
                    sb.append("  Xinha.startEditors(xinha_editors);\n");
                    sb.append("}\n");
                    sb.append("Xinha._addEvent(window,'load', xinha_init);\n");

                    response.renderJavascript(sb, null);
                }
            },

            new IHeaderContributor() {
                private static final long serialVersionUID = 1L;
                public void renderHead(IHeaderResponse response) {
                    response.renderOnLoadJavascript("xinha_init();");
                }
            }

        };
    }
}
