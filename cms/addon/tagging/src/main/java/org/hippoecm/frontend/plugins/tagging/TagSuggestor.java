package org.hippoecm.frontend.plugins.tagging;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.tagging.providers.AllTagsProvider;
import org.hippoecm.frontend.plugins.tagging.providers.ITagsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagSuggestor implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagSuggestor.class);

    private ArrayList<ITagsProvider> providers;
    
    public TagSuggestor() {
        providers = new ArrayList<ITagsProvider>();
        providers.add(new AllTagsProvider());
    }
    
    public List getTags(JcrNodeModel nodeModel){
        
        return null;
    }

    public void onFlush(JcrNodeModel nodeModel) {
        // TODO Auto-generated method stub

    }

}
