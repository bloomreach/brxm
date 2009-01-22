package org.hippoecm.hst.core.template.module.execution;

public class NOOPExecutionResultImpl implements ExecutionResult{

	private boolean success = true;
	private String message;
	
	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSuccess() {
		return success;
	}

}
