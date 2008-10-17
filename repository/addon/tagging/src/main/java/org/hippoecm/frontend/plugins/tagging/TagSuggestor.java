package org.hippoecm.frontend.plugins.tagging;

import java.util.ArrayList;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.tagging.providers.AllTagsProvider;
import org.hippoecm.frontend.plugins.tagging.providers.ITagsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagSuggestor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagSuggestor.class);

    private ArrayList<ITagsProvider> providers;
    private TagCollection tags;
    
    public TagSuggestor() {
        providers = new ArrayList<ITagsProvider>();
        providers.add(new AllTagsProvider());
        tags = new TagCollection();
    }
    
    public TagCollection getTags(JcrNodeModel nodeModel){
        
        for (ITagsProvider provider : providers){
            TagCollection tags = provider.getTags(nodeModel);
            this.tags.addAll(tags);
        }
        return tags;
    }
    

}
