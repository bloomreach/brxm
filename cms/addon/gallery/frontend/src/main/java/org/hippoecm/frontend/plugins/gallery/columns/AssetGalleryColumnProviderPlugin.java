package org.hippoecm.frontend.plugins.gallery.columns;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.columns.compare.CalendarComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.MimeTypeComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.SizeComparator;
import org.hippoecm.frontend.plugins.gallery.columns.modify.MimeTypeAttributeModifier;
import org.hippoecm.frontend.plugins.gallery.columns.render.DatePropertyRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.SizeRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.StringPropertyRenderer;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;

public class AssetGalleryColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String primaryItemName;

    public AssetGalleryColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        primaryItemName = config.getString("primaryItemName", "hippogallery:asset");
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        ListColumn<Node> column = new ListColumn<Node>(new ClassResourceModel("assetgallery-type", Translations.class),
                "type");
        column.setRenderer(new EmptyRenderer<Node>());
        column.setAttributeModifier(new MimeTypeAttributeModifier());
        column.setComparator(new MimeTypeComparator("jcr:mimeType", primaryItemName));
        column.setCssClass("assetgallery-type");
        columns.add(column);

        column = new ListColumn<Node>(new ClassResourceModel("assetgallery-name", Translations.class), "name");
        column.setComparator(new NameComparator());
        column.setCssClass("assetgallery-name");
        columns.add(column);

        return columns;
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();

        //Filesize
        ListColumn<Node> column = new ListColumn<Node>(new ClassResourceModel("assetgallery-size", Translations.class),
                "size");
        column.setRenderer(new SizeRenderer("jcr:data", primaryItemName));
        column.setComparator(new SizeComparator("jcr:data", primaryItemName));
        column.setCssClass("assetgallery-size");
        columns.add(column);

        //Mimetype
        column = new ListColumn<Node>(new ClassResourceModel("assetgallery-mimetype", Translations.class), "mimetype");
        column.setRenderer(new StringPropertyRenderer("jcr:mimeType", primaryItemName));
        column.setComparator(new MimeTypeComparator("jcr:mimeType", primaryItemName));
        column.setCssClass("assetgallery-mimetype");
        columns.add(column);

        //Last modified date
        column = new ListColumn<Node>(new ClassResourceModel("assetgallery-lastmodified", Translations.class),
                "lastmodified");
        column.setRenderer(new DatePropertyRenderer("jcr:lastModified", primaryItemName));
        column.setComparator(new CalendarComparator("jcr:lastModified", primaryItemName));
        column.setCssClass("assetgallery-lastmodified");
        columns.add(column);

        return columns;
    }
}
