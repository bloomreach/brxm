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

package org.hippoecm.hst.plugins.frontend.editor.sitemap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.BasicEditorPlugin;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.SitemapItemDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.HstContentPickerDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;
import org.hippoecm.hst.plugins.frontend.editor.validators.UniqueSitemapItemValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SitemapItemEditorPlugin extends BasicEditorPlugin<SitemapItem> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(SitemapItemEditorPlugin.class);

    private static final String BROWSE_LABEL = "[...]";

    public SitemapItemEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        FormComponent fc;
        fc = new RequiredTextField("matcher");
        fc.add(new UniqueSitemapItemValidator(this, hstContext.sitemap));
        form.add(fc);

        DropDownChoice ddc = new DropDownChoice("page", hstContext.page.getPagesAsList());
        ddc.setNullValid(false);
        ddc.setRequired(true);
        ddc.setOutputMarkupId(true);
        form.add(ddc);

        // Linkpicker
        final List<String> nodetypes = new ArrayList<String>();
        if (config.getStringArray("nodetypes") != null) {
            String[] nodeTypes = config.getStringArray("nodetypes");
            nodetypes.addAll(Arrays.asList(nodeTypes));
        }
        if (nodetypes.size() == 0) {
            log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
        }

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                String path = hstContext.sitemap.decodeContentPath(bean.getContentPath());
                Model model = new Model(hstContext.content.absolutePath(path));
                return new HstContentPickerDialog(context, config, model, nodetypes) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void saveNode(Node node) {
                        try {
                            bean.setContentPath(hstContext.content.relativePath(node.getPath()));
                            redraw();
                        } catch (RepositoryException e) {
                            log.error(e.getMessage());
                        }
                    }
                };
            }
        };

        DialogLink link = new DialogLink("path", new Model() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getObject() {
                return BROWSE_LABEL;
            }
        }, dialogFactory, getDialogService());
        link.setOutputMarkupId(true);
        form.add(link);

        TextField contentPath = new TextField("contentPath");
        contentPath.setOutputMarkupId(true);
        form.add(contentPath);
    }

    //FIXME: This is a nasty hack
    @Override
    protected String getAddDialogTitle() {
        return new StringResourceModel("dialog.title", this, null).getString();
    }

    @Override
    protected EditorDAO<SitemapItem> newDAO() {
        return new SitemapItemDAO(getPluginContext(), getPluginConfig());
    }

    @Override
    protected Dialog newAddDialog() {
        return new AddSitemapItemDialog(dao, this, (JcrNodeModel) getModel());
    }

}
