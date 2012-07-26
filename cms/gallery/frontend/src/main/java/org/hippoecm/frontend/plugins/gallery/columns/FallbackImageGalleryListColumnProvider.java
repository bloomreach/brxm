package org.hippoecm.frontend.plugins.gallery.columns;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.gallery.columns.modify.ImageGalleryIconModifier;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;

public class FallbackImageGalleryListColumnProvider implements IListColumnProvider {

    public IHeaderContributor getHeaderContributor() {
        return null;
    }

    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), null);
        column.setRenderer(new EmptyRenderer<Node>());
        column.setAttributeModifier(new ImageGalleryIconModifier());
        column.setCssClass("image-gallery-icon");
        columns.add(column);

        column = new ListColumn<Node>(new ClassResourceModel("gallery-name", Translations.class), "name");
        column.setComparator(new NameComparator());
        column.setCssClass("gallery-name");
        columns.add(column);

        return columns;
    }

    public List<ListColumn<Node>> getExpandedColumns() {
        return getColumns();
    }
}
