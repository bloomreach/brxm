package org.hippoecm.hst.core.template.module.execution;

public class NOOPExecutionResultImpl implements ExecutionResult{

	private boolean succes = true;
	private String message;
	
	public void setSucces(boolean succes) {
		this.succes = succes;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSucces() {
		return succes;
	}

}
