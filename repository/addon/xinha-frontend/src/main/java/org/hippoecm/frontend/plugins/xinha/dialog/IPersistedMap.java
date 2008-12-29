package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.Map;

import org.apache.wicket.IClusterable;

public interface IPersistedMap extends Map, IClusterable {
    
    boolean isValid();
    
    boolean hasChanged();

    boolean isExisting();

    void save();
    
    void delete();

    String toJsString();

}
