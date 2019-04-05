/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class OpenuiDialogCtrl {
  constructor($element, $log, $mdDialog, $scope, ExtensionService, locals, OpenUiService) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$mdDialog = $mdDialog;
    this.$scope = $scope;
    this.ExtensionService = ExtensionService;
    this.locals = locals;
    this.OpenUiService = OpenUiService;
  }

  $onInit() {
    try {
      this.connection = this.OpenUiService.initialize(this.locals.extensionId, {
        url: this.getUrl(),
        appendTo: this.$element.find('md-dialog-content')[0],
        methods: {
          cancelDialog: this.cancel.bind(this),
          closeDialog: this.closeDialog.bind(this),
          getDialogOptions: this.getDialogOptions.bind(this),
        },
      });
      this.$scope.$on('$destroy', this.destroyConnection.bind(this));
    } catch (error) {
      this.$log.warn(`Dialog '${this.locals.dialogOptions.title}' failed to connect with the client library.`, error);
      throw error;
    }
  }

  closeDialog(value) {
    return this.$mdDialog.hide(value);
  }

  cancel() {
    return this.$mdDialog.cancel();
  }

  destroyConnection() {
    if (this.connection) {
      this.connection.destroy();
    }
  }

  dialogSize() {
    return this.locals.dialogOptions.size || 'medium';
  }

  getUrl() {
    const extension = this.ExtensionService.getExtension(this.locals.extensionId);
    return this.ExtensionService.getExtensionRelativeUrl(extension, this.locals.dialogOptions.url);
  }

  getDialogOptions() {
    return this.locals.dialogOptions;
  }
}

export default OpenuiDialogCtrl;
