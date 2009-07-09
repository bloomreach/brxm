/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.dialog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copied from https://issues.apache.org/jira/browse/WICKET-12
 * Code by Maurice Marrink to allow a modal window to render on a non-ajax request.
 */
public class PersistentModalWindow extends ModalWindow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PersistentModalWindow.class);

    private transient Method callOnBeforeRenderIfNotVisibleMethod;
    private transient Field showField;

    private boolean renderScript = false;

    /**
    * @param id
    */
    @SuppressWarnings("unchecked")
    public PersistentModalWindow(String id) {
        super(id);
        initShowField();
        initOnBeforeRenderIfNotVisibleMethod();
    }

    /**
    *
    */
    private void initShowField() {
        try {
            showField = ModalWindow.class.getDeclaredField("shown");
            showField.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to get at shown field", e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unable to get at shown field", e);
        }
    }

    /**
    *
    */
    private void initOnBeforeRenderIfNotVisibleMethod() {
        try {
            callOnBeforeRenderIfNotVisibleMethod = Component.class.getDeclaredMethod("callOnBeforeRenderIfNotVisible",
                    new Class[0]);
            callOnBeforeRenderIfNotVisibleMethod.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to get callOnBeforeRenderIfNotVisible method", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to get callOnBeforeRenderIfNotVisible method", e);
        }
    }

    /**
    * @see org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow#onBeforeRender()
    */
    @Override
    protected void onBeforeRender() {
        boolean shown = isShown();
        super.onBeforeRender();
        // fix stupid parent behavior for non ajax requests
        if (shown && !isShown()) {
            show();
            Component content = get(getContentId());
            content.setVisible(true);

            // invoke before render on content; this has been skipped in parents onBeforeRender()
            Boolean result;
            try {
                result = (Boolean) callOnBeforeRenderIfNotVisibleMethod.invoke(content, new Object[0]);
                if (!result) {
                    content.beforeRender();
                }
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                log.error(e.getMessage(), e);
            }
            renderScript = true;
        }
    }

    /**
    * @see org.apache.wicket.markup.html.panel.Panel#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
    */
    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);
        // hack to open modal window in non ajax requests too
        if (renderScript) {
            try {
                Method m = ModalWindow.class.getDeclaredMethod("getWindowOpenJavascript");
                m.setAccessible(true);
                String script = (String) m.invoke(this);
                // hack to disable onclose warning for just this window
                container.getHeaderResponse().renderOnDomReadyJavascript(script);
            } catch (SecurityException e) {
                log.error(e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                log.error(e.getMessage(), e);
            }
            renderScript = false;
        }
    }

    /**
    * shows this modaldialog.
    */
    protected void show() {
        try {
            if (showField == null)
                initShowField();
            showField.set(this, true);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }
}
