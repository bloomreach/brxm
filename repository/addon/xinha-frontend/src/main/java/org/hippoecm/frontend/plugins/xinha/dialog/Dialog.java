package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Dialog implements IDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Dialog.class);

    private static final IDialogListener EMPTY_LISTENER = new IDialogListener() {
        private static final long serialVersionUID = 1L;

        public void onDialogClose() {
            log.warn("No IDialogListener configured.");
        }

        public String onDialogOk() {
            log.warn("No IDialogListener configured.");
            return null;
        }

        public void render(PluginRequestTarget target) {
        }
    };

    private IDialogListener listener;
    private ModalWindow modal;
    
    private int defaultWidth = 450;
    private int defaultHeight = 300;
    private boolean defaultResizable = true;

    public Dialog(String id) {
        modal = createModal(id);
        modal.setCloseButtonCallback(new CloseButtonCallback() {
            private static final long serialVersionUID = 1L;
            
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                cancel(target);
                return false;
            }
            
        });
        
        modal.setInitialWidth(defaultWidth);
        modal.setInitialHeight(defaultHeight);
        modal.setResizable(defaultResizable);
        
        modal.setCookieName("Dialog" + id); //TODO: test of +id is ok
        modal.setTitle("Dialog[" + id + "]");
        modal.setContent(new Panel(modal.getContentId()));

        listener = EMPTY_LISTENER;
    }
    
    protected ModalWindow createModal(String id) {
        return new ModalWindow(id);
    }

    public ModalWindow getModal() {
        return modal;
    }
    
    public void setListener(IDialogListener listener) {
        this.listener = listener;
    }

    /**
     * User closes Dialog by clicking OK button
     */
    public void ok(AjaxRequestTarget target) {
        String returnValue = listener.onDialogOk();
        close(target, getCloseScript(returnValue));
    }

    public void cancel(AjaxRequestTarget target) {
        close(target, getCancelScript());
    }
    
    private void close(AjaxRequestTarget target, String closeScript) {
        target.getHeaderResponse().renderOnDomReadyJavascript(closeScript);
        modal.close(target);
        listener.onDialogClose();
    }
    
    public void show(AjaxRequestTarget target) {
        modal.show(target);
    }
    
    
    protected abstract String getCloseScript(String returnValue);

    protected abstract String getCancelScript();

}
