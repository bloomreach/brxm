package org.onehippo.repository.documentworkflow.action;

import java.util.Calendar;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.task.RetrieveVersionTask;

/**
 * VersionVariantAction delegating the execution to VersionVariantTask.
 */
public class RetrieveVersionAction extends AbstractDocumentTaskAction<RetrieveVersionTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    public void setVariant(String variant) {
        setParameter("variantExpr", variant);
    }

    public String getHistoric() {
        return getParameter("historicExpr");
    }

    public void setHistoric(String variant) {
        setParameter("historicExpr", variant);
    }

    @Override
    protected RetrieveVersionTask createWorkflowTask() {
        return new RetrieveVersionTask();
    }

    @Override
    protected void initTask(RetrieveVersionTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVariant((DocumentVariant) eval(getVariant()));
        task.setHistoric((Calendar) eval(getHistoric()));
    }
}
