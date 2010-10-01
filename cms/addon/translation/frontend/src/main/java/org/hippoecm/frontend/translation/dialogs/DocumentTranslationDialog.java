package org.hippoecm.frontend.translation.dialogs;

import java.util.List;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.translation.FolderTranslation;
import org.hippoecm.frontend.translation.TranslationWorkflowPlugin;

public class DocumentTranslationDialog extends WorkflowAction.WorkflowDialog {
    private static final long serialVersionUID = 1L;

    private final TranslationWorkflowPlugin translationWorkflowPlugin;

    private IModel<String> title;
    private TextField<String> nameComponent;
    private TextField<String> uriComponent;
    private boolean uriModified;
    private List<FolderTranslation> folders;

    public DocumentTranslationDialog(TranslationWorkflowPlugin translationWorkflowPlugin, WorkflowAction action,
            IModel<String> title, List<FolderTranslation> folders) {
        action.super();
        this.translationWorkflowPlugin = translationWorkflowPlugin;
        this.title = title;
        this.folders = folders;

        DocumentTranslationView dtv = new DocumentTranslationView("grid", folders);
        dtv.setFrame(false);
        //        dtv.setWidth(600);
        add(dtv);
    }

    public IModel getTitle() {
        return title;
    }

    @Override
    protected void onDetach() {
        for (FolderTranslation ft : folders) {
            ft.detach();
        }
        super.onDetach();
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=675,height=450").makeImmutable();
    }
}
