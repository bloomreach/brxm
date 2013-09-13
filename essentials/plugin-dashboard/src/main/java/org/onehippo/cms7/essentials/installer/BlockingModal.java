/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.installer;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: BlockingModal.java 164013 2013-05-11 14:05:39Z mmilicevic $"
 */
public class BlockingModal extends ModalWindow  {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(BlockingModal.class);

    public BlockingModal(String id) {
        super(id);
    }


   /* @Override
    public void renderHead(final IHeaderResponse response) {

        final String jQueryString = "console.log('hoho');$('#blockingPopupLink').trigger('click');console.log('hoho');";
        response.render(JavaScriptHeaderItem.forScript(jQueryString, "BlockingModalScript"));
    }*/

}
