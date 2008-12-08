package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.wicket.IClusterable;

public interface IValue<K> extends IClusterable {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";
    
    K get();
    void set(K object);
    
    boolean isValid();
    String getScriptValue();

}
