/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.util.io.IClusterable;

public class DialogAction implements IClusterable {

    final private IDialogFactory factory;
    final private IDialogService dialogService;
    private boolean enabled = true;

    public DialogAction(final IDialogFactory dialogFactory, final IDialogService dialogService) {
        this.dialogService = dialogService;
        factory = dialogFactory;
    }

    public void execute() {
        if (!dialogService.isShowingDialog()) {
            dialogService.show(factory.createDialog());
        }
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
