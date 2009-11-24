package org.hippoecm.frontend.editor.plugins.field;

import java.util.Set;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.IValidationListener;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.Violation;

public class ValidationModel implements IModel<IValidationResult>, IObservable {
    private static final long serialVersionUID = 1L;

    private IObservationContext obContext;
    private IPluginContext context;
    private IPluginConfig config;
    private IValidationListener listener;

    public ValidationModel(IPluginContext context, IPluginConfig config) {
        this.config = config;
        this.context = context;
    }

    public void setObservationContext(IObservationContext<? extends IObservable> obContext) {
        this.obContext = obContext;
    }

    public void startObservation() {
        context.registerService(listener = new IValidationListener() {
            private static final long serialVersionUID = 1L;

            public void onResolve(Set<Violation> violations) {
                // TODO Auto-generated method stub

            }

            public void onValidation(IValidationResult result) {
                EventCollection collection = new EventCollection();
                collection.add(new IEvent<ValidationModel>() {

                    public ValidationModel getSource() {
                        return ValidationModel.this;
                    }

                });
                obContext.notifyObservers(collection);
            }

        }, config.getString(IValidationService.VALIDATE_ID));
    }

    public void stopObservation() {
        context.unregisterService(listener, config.getString(IValidationService.VALIDATE_ID));
        listener = null;
    }

    public IValidationResult getObject() {
        IValidationService service = context.getService(config.getString(IValidationService.VALIDATE_ID),
                IValidationService.class);
        if (service != null) {
            return service.getValidationResult();
        }
        return null;
    }

    public void setObject(IValidationResult object) {
        throw new UnsupportedOperationException("ValidationModel does not support setObject(Object)");
    }

    public void detach() {
    }

}
