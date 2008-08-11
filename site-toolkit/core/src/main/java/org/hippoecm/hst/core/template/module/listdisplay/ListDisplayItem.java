package org.hippoecm.hst.core.template.module.listdisplay;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.node.el.ELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListDisplayItem implements ELNode{
	private static final Logger log = LoggerFactory.getLogger(ListDisplayItem.class);
	
	Node node = null;
    public ListDisplayItem(Node node) {
    	this.node = node;
    }
    
    public Map getProperty() {
    	return new AbstractMap<String, Object>() {
    		@Override
    		public Set entrySet() {    			
    			return null;
    		}

			@Override
			public Object get(Object propertyName) {
				try {
					return node.getProperty((String) propertyName);
				} catch (PathNotFoundException e) {
				    log.error("property not found: " + (String) propertyName + " : " + e.getMessage());
				} catch (RepositoryException e) {					
				    log.error(e.getMessage());
				}
				return null;
			}
    	};
    }
    
    public String getPath() {
    	try {
			return node.getPath();
		} catch (RepositoryException e) {		
			log.error(e.getMessage(), e);
			return "";
	    }
    }
}
