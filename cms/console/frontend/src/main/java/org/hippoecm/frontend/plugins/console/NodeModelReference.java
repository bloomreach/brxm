package org.hippoecm.frontend.plugins.console;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

public class NodeModelReference implements IModelReference {

    private static final long serialVersionUID = 1L;
    private final Component component;
    private final JcrNodeModel model;

    public NodeModelReference(Component component, final JcrNodeModel model) {
        this.component = component;
        this.model = model;
    }

    @Override
    public IModel getModel() {
        return model;
    }

    @Override
    public void setModel(final IModel iModel) {
        component.setDefaultModel(iModel);
    }

    @Override
    public void detach() {
        // noop
    }

    @Override
    public void setObservationContext(final IObservationContext<? extends IObservable> context) {
        // noop
    }

    @Override
    public void startObservation() {
        // noop
    }

    @Override
    public void stopObservation() {
        // noop
    }
}
