package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.wicket.IClusterable;

public interface IValue<K> extends IClusterable {
    
    K get();
    void set(K object);
    
    boolean isValid();
    String getScriptValue();

}
