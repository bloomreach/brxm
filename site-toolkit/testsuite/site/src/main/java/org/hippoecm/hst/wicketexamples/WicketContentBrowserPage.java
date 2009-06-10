/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.wicketexamples;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WicketContentBrowserPage extends WebPage {
    
    private Logger logger = LoggerFactory.getLogger(WicketContentBrowserPage.class);

    protected String currentRelativePath = "";
    protected List<ItemBean> currentItemBeans = new ArrayList<ItemBean>();

    public WicketContentBrowserPage() {

        refreshCurrentPathItemBeans();
        
        Label currentRelativePathLabel = new Label("currentRelativePath", new PropertyModel(this, "currentRelativePath"));
        add(currentRelativePathLabel);
        
        Link parentLink = new Link("parentLink") {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                if (currentRelativePath != null && !"".equals(currentRelativePath)) {
                    int offset = currentRelativePath.lastIndexOf('/');
                    
                    if (offset >= 0) {
                        currentRelativePath = currentRelativePath.substring(0, offset);
                        refreshCurrentPathItemBeans();
                    }
                }
            }
        };
        
        add(parentLink);
        
        final DataView itemView = new DataView("itemView", new ListDataProvider(currentItemBeans)) {
            
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                final ItemBean itemBean = (ItemBean) item.getModelObject();
                
                final String name = itemBean.getName();
                final String path = itemBean.getPath();
                String primaryNodeTypeName = null;
                String uuid = null;
                String content = null;
                
                if (itemBean instanceof NodeBean) {
                    NodeBean nodeBean = (NodeBean) itemBean;
                    primaryNodeTypeName = nodeBean.getPrimaryNodeTypeName();
                    uuid = nodeBean.getUuid();
                    
                    if ("hippostd:html".equals(primaryNodeTypeName)) {
                        Object [] props = nodeBean.getProperty("hippostd:content");
                        
                        if (props != null && props.length > 0) {
                            content = (String) props[0];
                        }
                    }
                }

                Link nameLink = null;
                
                if ("hippo:resource".equals(primaryNodeTypeName)) {
                    // download link
                    nameLink = new Link("nameLink") {
                        private static final long serialVersionUID = 1L;
    
                        public void onClick() {
                            currentRelativePath += ("/" + name);
                            getRequestCycle().setRequestTarget(new RedirectRequestTarget("/binaries" + path));
                        }
                    };
                } else {
                    // navigational link
                    nameLink = new Link("nameLink") {
                        private static final long serialVersionUID = 1L;
    
                        public void onClick() {
                            currentRelativePath += ("/" + name);
                            refreshCurrentPathItemBeans();
                        }
                    };
                }
                
                nameLink.add(new Label("name", name));
                item.add(nameLink);
                item.add(new Label("primaryNodeTypeName", primaryNodeTypeName));
                item.add(new Label("uuid", uuid));
                item.add(new Label("content", content).setEscapeModelStrings(false));
            }
        };
        
        itemView.setItemsPerPage(10);
        add(itemView);
        add(new PagingNavigator("itemNavigator", itemView));
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setEscapeModelStrings(false);
        add(feedback);
        
    }
    
    public String getCurrentRelativePath() {
        return currentRelativePath;
    }
    
    protected void refreshCurrentPathItemBeans() {
        Repository repository = null;
        Session session = null;
        
        try {
            WicketContentBrowserApplication app = (WicketContentBrowserApplication) getApplication();
            repository = app.getDefaultRepository();
            Credentials credentials = app.getDefaultCredentials();
            String basePath = app.getBasePath();
            
            session = (credentials == null ? repository.login() : repository.login(credentials));
            
            String statement = basePath + currentRelativePath + (currentRelativePath.endsWith("/") ? "*" : "/*");
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, "xpath");
            QueryResult result = query.execute();
            
            currentItemBeans.clear();
            
            for (NodeIterator it = result.getNodes(); it.hasNext(); ) {
                currentItemBeans.add(ItemBeanFactory.createItemBean(it.nextNode()));
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to query.", e);
            }
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ce) {
                }
            }
        }
    }
    
}
