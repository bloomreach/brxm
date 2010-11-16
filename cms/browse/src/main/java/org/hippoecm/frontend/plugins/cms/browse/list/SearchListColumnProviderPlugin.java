package org.hippoecm.frontend.plugins.cms.browse.list;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class SearchListColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    public SearchListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>(1);

        //Path
        ListColumn<Node> column = new ListColumn<Node>(
                new ClassResourceModel("doclisting-path", SearchViewPlugin.class), "path");
        column.setRenderer(new AbstractNodeRenderer() {

            @Override
            protected Component getViewer(String id, Node node) throws RepositoryException {
                String path = node.getPath();
                return new Label(id, path);
            }
        });
        column.setCssClass("doclisting-path");
        columns.add(column);

        return columns;
    }
}
