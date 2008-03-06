package org.hippoecm.frontend.model;

import java.util.HashMap;
import java.util.Map;


public class ExceptionModel implements IPluginModel {

	private static final long serialVersionUID = 1L;
    private Exception exception; 
	
    public ExceptionModel(Exception e) {
		this.exception = e;
	}

    public Exception getException() {
		return exception;
	}
	

	public Map<String, Object> getMapRepresentation() {
		Map<String, Object> map = new HashMap<String, Object>();
        map.put("exception", exception);
        return map;
	}

	public Object getObject() {
		return exception;
	}

	public void setObject(Object object) {
		this.exception = (object instanceof Exception) ? (Exception)object : null;
	}

	public void detach() {
	}
}
