/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
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
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ReviewedActionsListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    private static final long serialVersionUID = 1L;

    public ReviewedActionsListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        return Collections.emptyList();
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<>();
        columns.add(createLastModifiedDateColumn());
        columns.add(createLastModifiedByColumn());
        columns.add(createPublicationDateColumn());
        return columns;
    }

    private ListColumn<Node> createLastModifiedDateColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-lastmodified-date", getClass());
        final ListColumn<Node> column = new ListColumn<Node>(displayModel, "lastmodified-date");
        column.setComparator(new DocumentAttributeComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return compareDates(s1.getLastModifiedDate(), s2.getLastModifiedDate());
            }
        });
        column.setCssClass(DocumentListColumn.DATE.getCssClass());
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
        return column;
    }

    private ListColumn<Node> createLastModifiedByColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-lastmodified-by", getClass());
        final ListColumn<Node> column = new ListColumn<Node>(displayModel, "lastmodified-by");
        column.setComparator(new DocumentAttributeComparator() {
            private static final long serialVersionUID = 3527258215486526567L;

            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                if (s1.getLastModifiedBy() == null || s2.getLastModifiedBy() == null)
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
        column.setCssClass(DocumentListColumn.LAST_MODIFIED_BY.getCssClass());
        return column;
    }

    private ListColumn<Node> createPublicationDateColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-publication-date", getClass());
        final ListColumn<Node> column = new ListColumn<Node>(displayModel, "publication-date");
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
        column.setCssClass(DocumentListColumn.DATE.getCssClass());
        return column;

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
