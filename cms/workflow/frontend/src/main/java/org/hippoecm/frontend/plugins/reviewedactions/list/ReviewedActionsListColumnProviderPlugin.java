/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.list;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.list.comparators.DocumentAttributeComparator;
import org.hippoecm.frontend.plugins.reviewedactions.list.comparators.StateComparator;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.DocumentAttributeAttributeModifier;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.DocumentAttributeRenderer;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewedActionsListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(ReviewedActionsListColumnProviderPlugin.class);
    private static final CssResourceReference COLUMN_SKIN = new CssResourceReference(ReviewedActionsListColumnProviderPlugin.class, "style.css");

    public ReviewedActionsListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(CssHeaderItem.forReference(COLUMN_SKIN));
            }
        };
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();
        ListColumn<Node> column;

        //State
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-state", getClass()), "state");
        column.setComparator(new StateComparator());
        column.setRenderer(new EmptyRenderer<Node>());
        column.setAttributeModifier(new StateIconAttributeModifier());
        column.setCssClass("doclisting-state");
        columns.add(column);

        return columns;
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();
        ListColumn<Node> column;

        //Date last modified
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-lastmodified-date", getClass()),
                "lastmodified-date");
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
                return formattedCalendarByStyle(atts.getLastModifiedDate(), "MS");
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
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-lastmodified-by", getClass()),
                "lastmodified-by");
        column.setComparator(new DocumentAttributeComparator() {
            private static final long serialVersionUID = 3527258215486526567L;

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                if(s1.getLastModifiedBy() == null || s2.getLastModifiedBy() == null)
                    return 0;
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
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-publication-date", getClass()),
                "publication-date");
        column.setComparator(new DocumentAttributeComparator() {
            private static final long serialVersionUID = -3201733296324543659L;

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return compareDates(s1.getPublicationDate(), s2.getPublicationDate());
            }
        });
        column.setRenderer(new DocumentAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(StateIconAttributes atts) {
                return formattedCalendarByStyle(atts.getPublicationDate(), "MS");
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

    private String formattedCalendarByStyle(Calendar calendar, String patternStyle) {
        if (calendar != null) {
            DateTimeFormatter dtf = DateTimeFormat.forStyle(patternStyle).withLocale(getLocale());
            return dtf.print(new DateTime(calendar));
        }
        return StringUtils.EMPTY;
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
            DateTimeFormatter dtf = DateTimeFormat.forStyle("LS").withLocale(getLocale());
            return dtf.print(new DateTime(cal));
        }
        return StringUtils.EMPTY;
    }

    private Locale getLocale() {
        return Session.get().getLocale();
    }

}
