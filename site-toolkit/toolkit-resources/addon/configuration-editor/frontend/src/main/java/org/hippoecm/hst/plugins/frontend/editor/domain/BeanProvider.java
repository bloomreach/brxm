package org.hippoecm.hst.plugins.frontend.editor.domain;

public interface BeanProvider<K extends EditorBean> {

    K getBean();

}
