package org.hippoecm.frontend.plugins.tagging.providers;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.tagging.TagCollection;
import org.hippoecm.frontend.plugins.tagging.editor.TagsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTagsProvider implements ITagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(AbstractTagsProvider.class);

    public final static String SERVICE = "service";

    public AbstractTagsProvider(IPluginContext context, IPluginConfig config) {
        String service = config.getString(SERVICE);
        context.registerService(this, service);
        log.debug("Registered under " + service);
    }

    abstract public TagCollection getTags(JcrNodeModel nodeModel);

    protected ArrayList<String> getCurrentTags(JcrNodeModel nodeModel) throws RepositoryException {
        ArrayList<String> tags = new ArrayList<String>();
        Node document = nodeModel.getNode();
        if (document.hasProperty(TagsPlugin.FIELD_NAME)) {
            Value[] tagsValues = document.getProperty(TagsPlugin.FIELD_NAME).getValues();
            for (Value tagValue : tagsValues) {
                tags.add(tagValue.getString());
            }
        }
        return tags;
    }
/*
    protected TagCollection searchRelatedTags(TagCollection collection, double relatedScore){
        
    }*/
}
