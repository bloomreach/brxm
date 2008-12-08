package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.commons.lang.builder.ToStringBuilder;

public abstract class Value<K> implements IValue<K> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    K value;
    boolean allowNull = false;
    boolean skip = false;

    public Value(K value) {
        this.value = value;
    }

    public final K get() {
        return value;
    }

    public final void set(K value) {
        this.value = value;
    }

    public boolean isValid() {
        if ((value == null && !allowNull) || skip) {
            return false;
        }
        return true;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("value", value).append("skip", skip).append("allowNull", allowNull)
                .toString();
    }

}
