package org.hippoecm.frontend.plugins.console;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

public class NodeModelReference implements IModelReference {

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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setObservationContext(final IObservationContext<? extends IObservable> context) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void startObservation() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stopObservation() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
