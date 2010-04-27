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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
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
import org.hippoecm.frontend.plugins.yui.layout.UnitExpandCollapseBehavior;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DocumentListingPlugin extends AbstractListingPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentListingPlugin.class);

    private static final String DOCUMENT_LISTING_CSS = "DocumentListingPlugin.css";
    private static final String TOGGLE_FULLSCREEN_IMG = "but-small.png";

    public DocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new AjaxLink<String>("toggleFullscreen", new Model<String>()) {
            private static final long serialVersionUID = 8830106986441125452L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                toggleFullscreen(target);
            }

        }.add(new Image("toggleFullscreenImage", TOGGLE_FULLSCREEN_IMG)));

    }

    private void toggleFullscreen(AjaxRequestTarget target) {
        MarkupContainer c = getParent();
        while(c != null) {
            for(IBehavior b : c.getBehaviors()) {
                if(b instanceof UnitExpandCollapseBehavior) {
                    ((UnitExpandCollapseBehavior) b).toggle(target);
                }
            }
            c = c.getParent();
        }
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        container.getHeaderResponse().renderCSSReference(new ResourceReference(DocumentListingPlugin.class, DOCUMENT_LISTING_CSS));
    }

    @Override
    protected TableDefinition<Node> getTableDefinition() {
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

        //Type
        column = new ListColumn<Node>(new StringResourceModel("doclisting-type", this, null), "type");
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
                Calendar o1 = s1.getLastModifiedDate();
                Calendar o2 = s2.getLastModifiedDate();
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
        });
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
                String o1 = s1.getLastModifiedBy();
                String o2 = s2.getLastModifiedBy();
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
        });
        column.setRenderer(new DocumentAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return atts.getLastModifiedBy();
            }
        });
        columns.add(column);

        //publication date
        column = new ListColumn<Node>(new StringResourceModel("doclisting-publication-date", this, null), "publication-date");
        column.setComparator(new DocumentAttributeComparator() {

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                Calendar o1 = s1.getPublicationDate();
                Calendar o2 = s2.getPublicationDate();
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
        columns.add(column);

        return new TableDefinition<Node>(columns);
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
        return new DraggebleListDataTable(id, tableDefinition, dataProvider, selectionListener, triState,
                pagingDefinition);
    }

    class DraggebleListDataTable extends ListDataTable<Node> {
        private static final long serialVersionUID = 1L;

        public DraggebleListDataTable(String id, TableDefinition<Node> tableDefinition, ISortableDataProvider<Node> dataProvider,
                TableSelectionListener<Node> selectionListener, boolean triState, ListPagingDefinition pagingDefinition) {
            super(id, tableDefinition, dataProvider, selectionListener, triState, pagingDefinition);

            //Don't use drag&drop on documents because 300+ documents in a folder on IE* is slow
            //            add(new DragBehavior(YuiPluginHelper.getManager(getPluginContext()), new DragSettings(YuiPluginHelper
            //                    .getConfig(getPluginConfig()))) {
            //                private static final long serialVersionUID = 1L;
            //
            //                @Override
            //                protected IModel getDragModel() {
            //                    return null;
            //                }
            //            });

            //add(new TableHelperBehavior());
        }
    }

}
