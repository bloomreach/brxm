package org.hippoecm.frontend.plugins.tagging.providers;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.tagging.TagCollection;

public interface ITagsProvider {
    final static String SVN_ID = "$Id$";
    
    public TagCollection getTags(JcrNodeModel nodeModel);
}
