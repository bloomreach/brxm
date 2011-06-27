package org.hippoecm.repository.ext;

public interface WorkflowManagerRegister {
    public <T> void bind(Class<T> contextClass, WorkflowInvocationHandlerModuleFactory<T> handlerClass);
}
