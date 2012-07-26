package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;

public class SearchListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    public SearchListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return CSSPackageResource.getHeaderContribution(SearchListColumnProviderPlugin.class, "SearchListColumnProviderPlugin.css");
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
