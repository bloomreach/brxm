package org.hippoecm.hst.core.template.module.listdisplay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListDisplayItem {
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
					e.printStackTrace();
				} catch (RepositoryException e) {					
					e.printStackTrace();
				}
				return null;
			}
    	};
    }
    
    public String getDate() {
    	 try {
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
			 Date newDate = node.getProperty("onehippo:date").getDate().getTime();
			 return dateFormat.format(newDate);
		} catch (Exception e) {		
			log.error(e.getMessage(), e);
			return "";
		}
    }
    
    public String getTitle() {
    	try {
    		return node.getProperty("onehippo:title").getString();
		} catch (Exception e) {		
			log.error(e.getMessage(), e);
			return "";
	    }
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
