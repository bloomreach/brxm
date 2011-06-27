package org.hippoecm.repository.quartz;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;

public abstract class AbstractSchedulerInvocationModule implements WorkflowInvocationHandlerModule {

    public AbstractSchedulerInvocationModule() {
    }

    public Object submit(WorkflowManager manager, WorkflowInvocation invocation) {
        try {
            if(SchedulerModule.log.isDebugEnabled()) {
                SchedulerModule.log.debug("Storing scheduled workflow {}",invocation.toString());
            }
            Scheduler scheduler = SchedulerModule.getScheduler(invocation.getSubject().getSession());
            Node subject = invocation.getSubject();
            Node handle = subject.getParent();
            if(handle.isNodeType("mix:versionable") && !handle.isCheckedOut()) {
                handle.checkout();
            }
            Node request = handle.addNode("hippo:request","hipposched:job");
            request.addMixin("mix:referenceable");

            String detail = request.getPath();
            JobDetail jobDetail = new JobDetail(detail, null, WorkflowJob.class);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("invocation", invocation);
            jobDataMap.put("document", invocation.getSubject().getUUID());
            jobDetail.setJobDataMap(jobDataMap);

            Trigger trigger = createTrigger(detail + "/default");
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (RepositoryException ex) {
            SchedulerModule.log.error("failure storing scheduled workflow", ex);
        } catch (SchedulerException ex) {
            SchedulerModule.log.error("failure storing scheduled workflow", ex);
        }
        return null;
    }

    protected abstract Trigger createTrigger(String name);

}
