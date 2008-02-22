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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.AbstractListingPlugin;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.datatable.ICustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.session.UserSession;

public class SearchPlugin extends AbstractListingPlugin{

    private static final String HIGHLIGHT = "highlight";
    private static final long serialVersionUID = 1L;
    private String query;
    private Label searchedFor;
    private Label didyoumean;
    private TextField field; 
    private JcrNodeModel model;
    
    public static final String SIMILAR = "similar";
    public static final String REP_EXCERPT = "rep:excerpt(.)";
    public static final String USER_PREF_NODENAME = "searchperspective-listingview";
   
    
    public SearchPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);
        this.model = (JcrNodeModel) getModel(); 
        
        final SearchForm form = new SearchForm("searchform");
        add(form);
        
        searchedFor = new Label("searchedfor", new Model(""));
        didyoumean = new Label("didyoumean", new Model(""));
        add(searchedFor);
        add(didyoumean);
        
    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }
    
    @Override
    protected ICustomizableDocumentListingDataTable getTable(IPluginModel model) {
        javax.jcr.Session session = ((UserSession)Session.get()).getJcrSession(); 
        
        dataTable = new CustomizableDocumentListingDataTable("table", columns, 
                new SortableQueryResultProvider(getQueryResult(session), session), pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        return dataTable;
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
    
    @Override
    protected IStyledColumn getNodeColumn(Model model, String propertyName, Channel channel) {
        return new SearchNodeColumn(model, propertyName, channel);
    }

    @Override
    protected void modifyDefaultPrefNode(Node prefNode, Channel channel) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException, ValueFormatException {
        super.modifyDefaultPrefNode(prefNode,channel);
        
        Node pref = prefNode.addNode(HIGHLIGHT,LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, HIGHLIGHT);
        pref.setProperty(PROPERTYNAME_PROPERTY, REP_EXCERPT);
        columns.add(getNodeColumn(new Model(HIGHLIGHT), REP_EXCERPT , channel));
        
        pref = prefNode.addNode(SIMILAR,LISTINGPROPS_NODETYPE);
        pref.setProperty(COLUMNNAME_PROPERTY, SIMILAR);
        pref.setProperty(PROPERTYNAME_PROPERTY, SIMILAR);
        columns.add(getNodeColumn(new Model(SIMILAR), SIMILAR , channel));
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
            searchedFor.setModelObject("You searched for : "  + query);
            SearchPlugin parent = (SearchPlugin)this.getParent(); 
            parent.remove((Component)dataTable);
            parent.add((Component)getTable(model));
        }
    }
}

