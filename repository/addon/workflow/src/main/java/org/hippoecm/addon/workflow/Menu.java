package org.hippoecm.addon.workflow;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;

public class Menu<T extends Menu> extends AbstractList<T> implements List<T>, Serializable {
    private String name;
    List<? extends Menu> submenus;

    public Menu(String name) {
        this.name = name;
        this.submenus = new LinkedList<Menu>();
    }

    public Menu(String name, List<? extends Menu> structure) {
        this.name = name;
        this.submenus = structure;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public T get(int index) {
        return (T) submenus.get(index);
    }
    
    @Override
    public int size() {
        return submenus.size();
    }
}
