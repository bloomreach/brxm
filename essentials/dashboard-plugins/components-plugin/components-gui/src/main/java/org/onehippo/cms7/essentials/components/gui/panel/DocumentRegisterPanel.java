package org.onehippo.cms7.essentials.components.gui.panel;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DocumentRegisterPanel extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DocumentRegisterPanel.class);
    private final ListMultipleChoice<String> availableDocuments;
    private List<String> selectedDocuments;
    private DocumentTemplateModel model;
    private final DashboardPlugin parent;

    public DocumentRegisterPanel(final DashboardPlugin  parent, final String id) {
        super(id);
        this.parent = parent;

        final Form<?> form = new Form("form");
        final List<String> items = new ArrayList<>();// TODO populate
        items.add("newsdocument");
        items.add("eventsdocument");
        final PropertyModel<List<String>> listModel = new PropertyModel<>(this, "selectedDocuments");
        availableDocuments = new ListMultipleChoice<>("documentTypes", listModel, items);
        availableDocuments.setOutputMarkupId(true);
        availableDocuments.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                log.debug("selectedDocuments {}", selectedDocuments);
            }
        });
        form.add(availableDocuments);
        add(form);



    }




    @Override
    public void applyState() {
        setComplete(false);
        log.info("@INSTALLING DOCUMENTS", selectedDocuments);
        if(selectedDocuments !=null && selectedDocuments.size() >0){
            final PluginContext context = parent.getContext();
            for (String selectedDocument : selectedDocuments) {
                final String prefix = context.getProjectNamespacePrefix();
                try {
                    final String superType = String.format("%s:basedocument", prefix);
                    CndUtils.registerDocumentType(context, prefix, selectedDocument, true, false, superType,"hippostd:relaxed");
                } catch (RepositoryException e) {
                    log.error(String.format("Error registering document type: %s", selectedDocument), e);
                }
            }

        }
        else{
            // skip installing documents
            log.info("No selected documents");
        }

        setComplete(true);

    }
}


