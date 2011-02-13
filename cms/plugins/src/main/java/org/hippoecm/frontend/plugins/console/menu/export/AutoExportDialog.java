/*
 *  Copyright 2011 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.menu.export;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoExportDialog extends AbstractDialog<Void> {

	private static final long serialVersionUID = 1L;
	private static final String NODE_PATH = "/hippo:configuration/hippo:frontend/console/console/menu/items/autoexport";
	private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");
	
	public AutoExportDialog(MenuPlugin plugin) {

		final String location = getExportLocation();
		
		// status label
		final Label statusLabel = new Label("status-label", new Model<String>() {
			private static final long serialVersionUID = 1L;

			@Override public String getObject() {
				if (location == null) {
					return "Export location is not set, automatic export is unavailable. To make export available, " +
							"startup the CMS with system property 'hippoecm.config.dir' pointing to the location " +
							"where you would like export to read and write hippoecm-extension.xml and related files.";
				}
				return isExportEnabled() ? "Export enabled, writing changes to " + location : "Export disabled";
			}
			
		});
		statusLabel.setOutputMarkupId(true);
        add(statusLabel);
		
        
    	// enable / disable link
		final Label actionLabel = new Label("action-link-text", new Model<String>() {
			private static final long serialVersionUID = 1L;
			
			@Override public String getObject() {
				if (location == null) {
					return "";
				}
				return isExportEnabled() ? "Disable export" : "Enable export";
			}

		});
		actionLabel.setOutputMarkupId(true);

        AjaxLink<Void> actionLink = new AjaxLink<Void>("action-link") {
            private static final long serialVersionUID = 1L;

            @Override public void onClick(AjaxRequestTarget target) {
            	// toggle export enabled flag
            	setExportEnabled(!isExportEnabled());
            	// update labels
            	target.addComponent(actionLabel);
            	target.addComponent(statusLabel);
            }
            
        };
        actionLink.setOutputMarkupId(true);
        actionLink.add(actionLabel);
        actionLink.setEnabled(location != null);
        
        add(actionLink);

        // dialog settings
		setOkLabel("Close");
		setCancelVisible(false);
		setFocusOnOk();
	}

	@Override
	public IModel<String> getTitle() {
		return new Model<String>("Auto Export configuration");
	}

	private boolean isExportEnabled() {
		boolean enabled = true;
		try {
			Node node = getJcrSession().getNode(NODE_PATH);
			enabled = node.getProperty("enabled").getValue().getBoolean();
		} catch (PathNotFoundException e) {
			log.warn("No such item: " + NODE_PATH + "/enabled");
		} catch (RepositoryException e) {
			log.error("An error occurred reading export enabled flag", e);
		}
		return enabled;
	}
	
	private void setExportEnabled(boolean enabled) {
		Session session = getJcrSession();
		try {
			// FIXME: we need to get a separate session from the one provided
			// otherwise other changes to the console will be persisted
			// upon save() as well
			//session = session.impersonate(new SimpleCredentials(?,?));
			Node node = session.getNode(NODE_PATH);
			node.setProperty("enabled", enabled);
			session.save();
			// session.logout();
		} catch (PathNotFoundException e) {
			log.warn("No such item: " + NODE_PATH + "/enabled");
		} catch (RepositoryException e) {
			log.error("An error occurred trying to set export enabled flag", e);
		}
	}
	
	private String getExportLocation() {
		String location = null;
		try {
			Node node = getJcrSession().getNode(NODE_PATH);
			location = node.getProperty("location").getString();
		} catch (PathNotFoundException e) {
			log.debug("No such item: " + NODE_PATH + "/location");
		} catch (RepositoryException e) {
			log.error("An error occurred looking up export location", e);
		}
		return location;
	}
	
	private Session getJcrSession() {
		Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
		return session;
	}
	

}
