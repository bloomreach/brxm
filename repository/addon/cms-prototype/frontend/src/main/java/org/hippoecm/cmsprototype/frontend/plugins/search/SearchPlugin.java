/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.list.AbstractListingPlugin;
import org.hippoecm.cmsprototype.frontend.plugins.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class SearchPlugin extends AbstractListingPlugin{
    private static final long serialVersionUID = 1L;
 
    public static final String USER_PREF_NODENAME = "hippo:searchperspective-listingview";
    
    private String query;
    private Label searchedFor;
    private Label didyoumean;
    private TextField field; 
    private List<SearchHit> hits = new ArrayList<SearchHit>(); 
    private JcrNodeModel model;
    
    public SearchPlugin(PluginDescriptor pluginDescriptor, final JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
        this.model = model; 
        final SearchForm form = new SearchForm("searchform");
        
        searchedFor = new Label("searchedfor", new Model(""));
        didyoumean = new Label("didyoumean", new Model(""));
       
        PageableListView pageableListView = new PageableListView("hits",hits, 30) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final SearchHit hit = (SearchHit)item.getModelObject() ;
                item.add(new Label("nodename", hit.getName()));
                item.add(new Label("path", hit.getPath()));
                System.out.println(hit.getExcerpt());
                item.add(new IncludeHtml("excerpt" , hit.getExcerpt()));
                item.add(new Link("similar") {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onClick() {
                        searchedFor.setModelObject("You searched for similar : "  + hit.getName());
                        newSearch(hits,"//element(*, hippo:document)[rep:similar(., '" + hit.getPath() + "')]/rep:excerpt(.)");
                    }
                    
                });
            }
        };
        
        add(form);
        add(searchedFor);
        add(didyoumean);
        add(pageableListView);
        
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }
    
    private final class SearchForm extends Form {

        private static final long serialVersionUID = 1L;
        
        public SearchForm(String id) {
            super(id);
            field = new TextField("searchtext",new Model(""));
            add(field);
            add(new Button("submit", new Model("search!!")));
        }
        public SearchForm(String id, IModel model) {
            super(id, model);
            field = new TextField("searchtext",new Model(""));
            add(field);
            add(new Button("submit", new Model("search!!")));
        }
        public void onSubmit() {
            query = (String)field.getModelObject();
            field.setModelObject(query);
            searchedFor.setModelObject("You searched for similar : "  + query);
            this.getParent().remove((Component)dataTable);
            ((SearchPlugin)this.getParent()).addTable(model, pageSize, viewSize);
        }
        
        
    }

    @Override
    protected void addTable(JcrNodeModel nodeModel, int pageSize, int viewSize) {
        javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession()); 
        
       dataTable = new CustomizableDocumentListingDataTable("table", columns, 
                new SortableQueryResultProvider(getQueryResult(session), session), pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        add((Component)dataTable); 
    
        
    }

    private QueryResult getQueryResult(javax.jcr.Session session){
        if(query == null) {
            return null;
        }
        String xpath = "//element(*,hippo:document)[jcr:contains(.,'"+query+"')]/rep:excerpt(.)";
        QueryResult result = null;
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
            result = q.execute();
            
            RowIterator rows = result.getRows();
            
            if(rows.getSize() == 0 ) {
                
                Value v = session.getWorkspace().getQueryManager().createQuery(
                        "//element(*, hippo:document)[rep:spellcheck('" + query + "')]/(rep:spellcheck())",
                        Query.XPATH).execute().getRows().nextRow().getValue("rep:spellcheck()");
                if (v != null) {
                    didyoumean.setModelObject("Did you mean : " + v.getString() );
                    field.setModelObject(v.getString());
                } else {
                    didyoumean.setModelObject("No results, no suggestions");
                }
            } else {
                didyoumean.setModelObject("");
            }
        } catch (InvalidQueryException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    
    
    
    private void newSearch(List<SearchHit> hits, String xpath) { 
        newSearch(hits, xpath, null);
    }
    
    private void newSearch(List<SearchHit> hits, String xpath, String value) {
        javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession());
        hits.clear();
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
            
            QueryResult result = q.execute();
            
            RowIterator rows = result.getRows();
            
            if(rows.getSize() == 0 && value != null) {
                
                Value v = session.getWorkspace().getQueryManager().createQuery(
                        "//element(*, hippo:document)[rep:spellcheck('" + value + "')]/(rep:spellcheck())",
                        Query.XPATH).execute().getRows().nextRow().getValue("rep:spellcheck()");
                if (v != null) {
                    didyoumean.setModelObject("Did you mean : " + v.getString() );
                    field.setModelObject(v.getString());
                } else {
                    didyoumean.setModelObject("No results, no suggestions");
                }
            } else {
                didyoumean.setModelObject("");
            }
      
            while(rows.hasNext()){
                hits.add(new SearchHit(rows.nextRow() , session ));
            }
           
        } catch (InvalidQueryException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        
    }
    
    
    
    class SearchHit implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private String path;
        private String excerpt;
        private String similar;
        
        public SearchHit(Node node) throws RepositoryException{
           this.name = node.getName();
           this.path = node.getPath();
        }

        public SearchHit(Row row, javax.jcr.Session session) throws ValueFormatException, IllegalStateException, ItemNotFoundException, RepositoryException {
            this.path = row.getValue("jcr:path").getString();
            if(row.getValue("rep:excerpt(.)") != null ){
                this.excerpt = row.getValue("rep:excerpt(.)").getString();
            }
            Node n = (Node) session.getItem(path);
            this.name = n.getName();
            
        }

        public String getExcerpt() {
            return excerpt;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getSimilar() {
            return similar;
        }
        
    }


    
}

