package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModifiedDocumentsView extends Panel implements IPagingDefinition {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ReferringDocumentsView.java 24007 2010-09-21 15:35:40Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ModifiedDocumentsView.class);

    private ListDataTable dataTable;
    private ModifiedDocumentsProvider provider;

    public ModifiedDocumentsView(String id, ModifiedDocumentsProvider provider) {
        super(id);

        setOutputMarkupId(true);
        this.provider = provider;
        add(new Label("message", new Model<String>("There are " + provider.size() + " modified document(s)")));

        dataTable = new ListDataTable("datatable", getTableDefinition(), this.provider, new ListDataTable.TableSelectionListener(){
            public void selectionChanged(IModel iModel) {
                //Do Nothing for now
            }
        }, true, this);
        add(dataTable);

        add(new CssClassAppender(new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (ModifiedDocumentsView.this.provider.size() == 0) {
                    return "hippo-empty";
                }
                return "";
            }
        }));


        add(new CssClassAppender(new Model<String>("hippo-referring-documents")));
    }


    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();


        ListColumn column = new ListColumn(new StringResourceModel("doclisting-name", this, null), null);
        column.setAttributeModifier(new DocumentAttributeModifier());
        columns.add(column);

        columns.add(column);

        return new TableDefinition(columns);
    }

    public int getPageSize() {
        return 7;
    }

    public int getViewSize() {
        return 5;
    }

}
