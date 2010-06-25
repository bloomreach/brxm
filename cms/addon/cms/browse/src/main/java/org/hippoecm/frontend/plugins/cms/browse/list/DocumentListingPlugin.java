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
package org.hippoecm.frontend.plugins.cms.browse.list;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.list.comparators.DocumentAttributeComparator;
import org.hippoecm.frontend.plugins.cms.browse.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.comparators.TypeComparator;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeRenderer;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableSettings;
import org.hippoecm.frontend.plugins.yui.layout.ExpandCollapseLink;
import org.hippoecm.frontend.plugins.yui.layout.IExpandableCollapsable;
import org.hippoecm.frontend.plugins.yui.datatable.DataTableBehavior;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DocumentListingPlugin extends AbstractListingPlugin implements IExpandableCollapsable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentListingPlugin.class);

    private static final String DOCUMENT_LISTING_CSS = "DocumentListingPlugin.css";
    private static final String TOGGLE_FULLSCREEN_IMG = "but-small.png";

    boolean isExpanded = false;

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final ExpandCollapseLink<String> link = new ExpandCollapseLink<String>("toggleFullscreen");
        link.add(new Image("toggleFullscreenImage", TOGGLE_FULLSCREEN_IMG));

        WebMarkupContainer c = new WebMarkupContainer("toggleContainer") {

            @Override
            public boolean isVisible() {
                return link.isVisible();
            }

            @Override
            public boolean isEnabled() {
                return link.isEnabled();
            }
        };

        c.add(link);
        add(c);
    }

    public void collapse() {
        isExpanded = false;
        onModelChanged();
    }

    public boolean isSupported() {
        return getPluginConfig().getAsBoolean("expand.collapse.supported", false);
    }

    public void expand() {
        isExpanded = true;
        onModelChanged();
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        container.getHeaderResponse().renderCSSReference(new ResourceReference(DocumentListingPlugin.class, DOCUMENT_LISTING_CSS));
    }

    @Override
    protected TableDefinition<Node> getTableDefinition() {
        return new TableDefinition<Node>(isExpanded ? getExpandedColumns() : getCollapsedColumns());
    }

    protected List<ListColumn<Node>> getCollapsedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        //Type Icon
        ListColumn<Node> column = new ListColumn<Node>(new Model(""), "icon");
        column.setComparator(new TypeComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IconAttributeModifier());
        column.setCssClass("doclisting-icon");
        columns.add(column);

        //Name
        column = new ListColumn<Node>(new StringResourceModel("doclisting-name", this, null), "name");
        column.setComparator(new NameComparator());
        column.setAttributeModifier(new DocumentAttributeModifier());
        column.setCssClass("doclisting-name");
        columns.add(column);

        //State
        column = new ListColumn<Node>(new StringResourceModel("doclisting-state", this, null), "state");
        column.setComparator(new StateComparator());
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new StateIconAttributeModifier());
        column.setCssClass("doclisting-state");
        columns.add(column);

        return columns;
    }

    protected List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getCollapsedColumns();

        //Type
        ListColumn<Node> column = new ListColumn<Node>(new StringResourceModel("doclisting-type", this, null), "type");
        column.setComparator(new TypeComparator());
        column.setRenderer(new TypeRenderer());
        column.setCssClass("doclisting-type");
        columns.add(column);

        //Date last modified
        column = new ListColumn<Node>(new StringResourceModel("doclisting-lastmodified-date", this, null), "lastmodified-date");
        column.setComparator(new DocumentAttributeComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return compareDates(s1.getLastModifiedDate(), s2.getLastModifiedDate());
            }
        });
        column.setCssClass("doclisting-date");
        column.setRenderer(new DocumentAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return simpleFormattedCalendar(atts.getLastModifiedDate());
            }
        });
        column.setAttributeModifier(new DocumentAttributeAttributeModifier("title") {
            private static final long serialVersionUID = 1036099861027058091L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return advancedFormattedCalendar(atts.getLastModifiedDate());
            }
        });

        columns.add(column);

        //Last modified by
        column = new ListColumn<Node>(new StringResourceModel("doclisting-lastmodified-by", this, null), "lastmodified-by");
        column.setComparator(new DocumentAttributeComparator() {

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return s1.getLastModifiedBy().compareTo(s2.getLastModifiedBy());
            }
        });
        column.setRenderer(new DocumentAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return atts.getLastModifiedBy();
            }
        });
        column.setCssClass("doclisting-lastmodified-by");
        columns.add(column);

        //publication date
        column = new ListColumn<Node>(new StringResourceModel("doclisting-publication-date", this, null), "publication-date");
        column.setComparator(new DocumentAttributeComparator() {

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return compareDates(s1.getPublicationDate(), s2.getPublicationDate());
            }
        });
        column.setRenderer(new DocumentAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return simpleFormattedCalendar(atts.getPublicationDate());
            }
        });
        column.setAttributeModifier(new DocumentAttributeAttributeModifier("title") {
            private static final long serialVersionUID = 1036099861027058091L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return advancedFormattedCalendar(atts.getPublicationDate());
            }
        });
        column.setCssClass("doclisting-date");
        columns.add(column);

        return columns;
    }

    private int compareDates(Calendar o1, Calendar o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return 1;
        } else if (o2 == null) {
            return -1;
        }
        return o1.compareTo(o2);
    }

    private String simpleFormattedCalendar(Calendar cal) {
        if (cal != null) {
            return DateTimeFormat.forPattern("d-MMM-yyyy").withLocale(getLocale()).print(new DateTime(cal));
        }
        return "";
    }

    private String advancedFormattedCalendar(Calendar cal) {
        if (cal != null) {
            return DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("LS", getLocale())).print(new DateTime(cal));
        }
        return "";
    }

    @Override
    protected ListDataTable<Node> getListDataTable(String id, TableDefinition<Node> tableDefinition,
            ISortableDataProvider<Node> dataProvider, TableSelectionListener<Node> selectionListener, boolean triState,
            ListPagingDefinition pagingDefinition) {

        ListDataTable<Node> datatable = super.getListDataTable(id, tableDefinition, dataProvider, selectionListener,
                triState, pagingDefinition);
        DataTableSettings settings = new DataTableSettings();
        settings.setAutoWidthColumnClassname("doclisting-name");
        datatable.add(new DataTableBehavior(settings));
        return datatable;
    }

}
