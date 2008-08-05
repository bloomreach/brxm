package org.hippoecm.hst.core.template.node.el;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Convenient base class for pseudo Map implementations that are used to enable function like syntax in standard EL.
 */
public abstract class ELPseudoMap implements Map {

    /**
     * Must be implementated by concrete sub classes to simulate "function calls" in EL.
     * @param key argument of the "function call"
     * @return result of the "function call"
     */
    public abstract Object get(Object key);


    // The rest of the Map interface is actually not used

    public int size() {
        return 1; //always return 1
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsKey(Object key) {
        return true;
    }

    public boolean containsValue(Object value) {
        return true;
    }

    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map t) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    public Collection values() {
        throw new UnsupportedOperationException();
    }

    public Set entrySet() {
        throw new UnsupportedOperationException();
    }
}
