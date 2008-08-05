package org.hippoecm.hst.core.template.module.login;

public class LoginBean {
	private boolean loggedIn;
	private String action;
	private String userId;
	
	public LoginBean() {
		loggedIn = true;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}

