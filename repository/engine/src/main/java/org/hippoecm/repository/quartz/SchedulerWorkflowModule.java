package org.hippoecm.repository.quartz;

import java.util.Date;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModuleFactory;
import org.hippoecm.repository.ext.WorkflowManagerModule;
import org.hippoecm.repository.ext.WorkflowManagerRegister;
import org.hippoecm.repository.quartz.CronSchedulerInvocationModule;
import org.quartz.CronExpression;

public class SchedulerWorkflowModule implements WorkflowManagerModule {
    public SchedulerWorkflowModule() {
    }

    @Override
    public void register(WorkflowManagerRegister register) {
        register.bind(Date.class, new WorkflowInvocationHandlerModuleFactory<Date>() {
            @Override
            public WorkflowInvocationHandlerModule createInvocationHandler(Date date) {
                return new DateSchedulerInvocationModule(date);
            }
        });
        register.bind(CronExpression.class, new WorkflowInvocationHandlerModuleFactory<CronExpression>() {
            @Override
            public WorkflowInvocationHandlerModule createInvocationHandler(CronExpression cronExpression) {
                return new CronSchedulerInvocationModule(cronExpression);
            }
        });
    }
}
