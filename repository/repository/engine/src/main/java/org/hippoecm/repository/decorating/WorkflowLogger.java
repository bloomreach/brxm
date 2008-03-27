package org.hippoecm.repository.decorating;

import java.lang.reflect.Method;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowLogger {
    final Logger log = LoggerFactory.getLogger(Workflow.class);

    private boolean enabled = false;
    private Node logFolder;
    
    public WorkflowLogger(Session session) {
        try {
            Node configuration = session.getRootNode().getNode(
                    HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.LOGGING_PATH);
            
            enabled = configuration.getProperty("hippo:logEnabled").getBoolean();
            if (enabled) {
                String logPath = configuration.getProperty("hippo:logPath").getString();
                if (!session.itemExists(logPath)) {
                    String logParentPath = logPath.substring(0, logPath.lastIndexOf("/"));
                    logParentPath = logParentPath.length() == 0 ? "/" : logParentPath;
                    if (session.itemExists(logParentPath)) {
                        Node logParent = (Node)session.getItem(logParentPath);
                        String logFolderName = logPath.substring(logPath.lastIndexOf("/")+1, logPath.length());
                        logFolder = logParent.addNode(logFolderName, "hippo:logfolder");
                        logFolder.getParent().save();
                        log.info("Workflow logging configuration: created logging base node '" + logPath + "'");
                    } else {
                        enabled = false;
                        log.error("Workflow logging configuration failed: logging base node '" + logPath + "' does not exist.");
                        return;
                    }
                }
                log.info("Workflow logging enabled: workflow steps will be logged to = '" + logPath + "'");
            } else {
                log.info("Workflow logging disabled, workflow steps will not be logged");                
            }
        } catch (RepositoryException e) {
            log.error("workflow logger configuration failed: " + e.getMessage(), e);
        }
    }

    public void log(String who, Workflow workflow, Method targetMethod, Object[] args, Object returnObject) {
        if (enabled) {
            try {
                Node logNode = logFolder.addNode(String.valueOf(System.currentTimeMillis()), "hippo:logitem");

                logNode.setProperty("hippo:who", who);
                logNode.setProperty("hippo:workflowClass", workflow.getClass().getName());
                logNode.setProperty("hippo:workflowMethod", targetMethod.getName());

                if (args != null) {
                    String[] arguments = new String[args.length];
                    for (int i = 0; i < args.length; i++) {
                        arguments[i] = args[i].toString();
                    }
                    logNode.setProperty("hippo:workflowArguments", arguments);
                }

                if (returnObject != null) {
                    logNode.setProperty("hippo:workflowReturnType", returnObject.getClass().getName());
                    logNode.setProperty("hippo:workflowReturnValue", returnObject.toString());
                }
                logFolder.save();

            } catch (RepositoryException e) {
                log.error("Workflow logging failed: " + e.getMessage(), e);
            }
        }
    }

}
