/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.wicketexamples;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.RequestContext;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example wicket web page is designed to demonstrate
 * how to access a JCR session pooling repository component provided by HST-2 container
 * and how an application can use simple pure JCR APIs for some purposes.
 * <P>
 * <EM>Please note that this example is not designed to support more advanced JCR repository features
 * such as workflow, virtual nodes, document templates or faceted navigation
 * which are supported by some JCR repository implementations.</EM> 
 * </P>
 * 
 * @version $Id: WicketContentBrowserPage.java 18499 2009-06-11 16:32:35Z wko $
 */
public class WicketContentBrowserPage extends WebPage {
    
    private Logger logger = LoggerFactory.getLogger(WicketContentBrowserPage.class);

    protected String basePath;
    protected int itemsPerPage = 10;
    protected String currentPath;
    protected String currentRelativePath;
    protected List<ItemBean> currentItemBeans = new ArrayList<ItemBean>();
    protected List<ItemBean> searchedItemBeans = new ArrayList<ItemBean>();
    
    protected String searchQuery;

    public WicketContentBrowserPage() {

        basePath = ((WebApplication) getApplication()).getInitParameter("basePath");
        
        if (basePath == null) {
            basePath = "/";
        }
        
        refreshCurrentPathItemBeans();
        
        Label currentRelativePathLabel = new Label("currentRelativePath", new PropertyModel(this, "currentRelativePath"));
        add(currentRelativePathLabel);
        
        Link parentLink = new Link("parentLink") {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                if (!"/".equals(currentPath)) {
                    int offset = currentPath.lastIndexOf('/');
                    
                    if (offset >= 0) {
                        currentPath = currentPath.substring(0, offset);

                        if ("".equals(currentPath)) {
                            currentPath = "/";
                        }
                        
                        refreshCurrentPathItemBeans();
                    }
                }
            }
        };
        
        add(parentLink);
        
        final DataView itemView = new ItemBeansDataView("itemView", new ListDataProvider(currentItemBeans)) {
            
            private static final long serialVersionUID = 1L;

            @Override
            protected void navigateToPath(String path) {
                currentPath = path;
                refreshCurrentPathItemBeans();
            }
            
        };
       
        String param = ((WebApplication) getApplication()).getInitParameter("itemsPerPage");
        
        if (param != null) {
            itemsPerPage = Math.max(1, Integer.parseInt(param.trim()));
        }
        
        itemView.setItemsPerPage(itemsPerPage);
        add(itemView);
        add(new PagingNavigator("itemNavigator", itemView));
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setEscapeModelStrings(false);
        add(feedback);
        
        Form form = new Form("searchForm") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                if (searchQuery != null && !"".equals(searchQuery.trim())) {
                    searchItems();
                }
            }
        };
        
        form.add(new TextField("searchQuery", new PropertyModel(this, "searchQuery")));
        form.add(new Button("search"));
        add(form);
        
        final DataView searchedItemView = new ItemBeansDataView("searchedItemView", new ListDataProvider(searchedItemBeans));
        
        searchedItemView.setItemsPerPage(itemsPerPage);
        add(searchedItemView);
        add(new PagingNavigator("searchedItemNavigator", itemView));
    }
    
    public String getCurrentRelativePath() {
        return currentRelativePath;
    }
    
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    protected void refreshCurrentPathItemBeans() {
        Repository repository = null;
        Session session = null;
        
        try {
            WicketContentBrowserApplication app = (WicketContentBrowserApplication) getApplication();
            repository = app.getDefaultRepository();
            Credentials credentials = app.getDefaultCredentials();
            
            session = (credentials == null ? repository.login() : repository.login(credentials));
            
            if (currentPath == null) {
                currentPath = basePath;
            }
            
            currentRelativePath = currentPath;
            
            if (!"/".equals(basePath) && currentPath.startsWith(basePath)) {
                currentRelativePath = currentPath.substring(basePath.length());
            }
            
            Node rootNode = session.getRootNode();
            Node currentNode = ("/".equals(currentPath) ? rootNode : rootNode.getNode(currentPath.startsWith("/") ? currentPath.substring(1) : currentPath));
            
            NodeIterator nodeIt = currentNode.getNodes();
            
            currentItemBeans.clear();
            
            for ( ; nodeIt.hasNext(); ) {
                currentItemBeans.add(ItemBeanFactory.createItemBean(nodeIt.nextNode()));
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Unable to retrieve nodes on " + currentRelativePath, e);
            }
            
            FeedbackPanel feed = (FeedbackPanel) getPage().get("feedback");
            feed.error("Unable to retrieve nodes on " + currentRelativePath + ": " + e.getMessage());
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ce) {
                }
            }
        }
    }
    
    protected void searchItems() {
        Repository repository = null;
        Session session = null;
        
        try {
            WicketContentBrowserApplication app = (WicketContentBrowserApplication) getApplication();
            repository = app.getDefaultRepository();
            Credentials credentials = app.getDefaultCredentials();
            
            session = (credentials == null ? repository.login() : repository.login(credentials));
            
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String statement = "//element(*, hippostd:html)[jcr:contains(., '" + searchQuery + "')]";
            Query query = queryManager.createQuery(statement, "xpath");
            QueryResult result = query.execute();
            NodeIterator nodeIt = result.getNodes();
            
            searchedItemBeans.clear();
            
            for ( ; nodeIt.hasNext(); ) {
                searchedItemBeans.add(ItemBeanFactory.createItemBean(nodeIt.nextNode()));
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Unable to search nodes by " + searchQuery, e);
            }
            
            FeedbackPanel feed = (FeedbackPanel) getPage().get("feedback");
            feed.error("Unable to search nodes by " + searchQuery + ": " + e.getMessage());
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ce) {
                }
            }
        }
    }
    
    protected class ItemBeansDataView extends DataView {
        
        private static final long serialVersionUID = 1L;

        protected ItemBeansDataView(String id, IDataProvider dataProvider) {
            super(id, dataProvider);
        }
        
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
                        final String redirectUrl = ((WicketContentBrowserApplication) getApplication()).getBinaryDownloadServletPath() + path;
                        getRequestCycle().setRequestTarget(new RedirectRequestTarget(redirectUrl) {
                            @Override
                            public void respond(RequestCycle requestCycle) {
                                Response response = requestCycle.getResponse();
                                response.reset();
                                RequestContext rc = RequestContext.get();
                                response.redirect(redirectUrl);
                            }
                        });
                    }
                };
            } else {
                // navigational link
                nameLink = new Link("nameLink") {
                    private static final long serialVersionUID = 1L;

                    public void onClick() {
                        navigateToPath(path);
                    }
                };
            }
            
            nameLink.add(new Label("name", name));
            item.add(nameLink);
            item.add(new Label("primaryNodeTypeName", primaryNodeTypeName));
            item.add(new Label("uuid", uuid));
            item.add(new Label("content", content).setEscapeModelStrings(false));
        }
        
        protected void navigateToPath(String path) {
        }
        
    }
}
