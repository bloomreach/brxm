/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

const MIN_HEIGHT_IN_PIXELS = 10;
const MAX_HEIGHT_IN_PIXELS = 10000;
const MAX_SIZE_IN_BYTES = 102400;

export default class OpenuiStringFieldController {
  constructor($element, $log, CmsService, ContentEditor, DialogService, ExtensionService, OpenUiService) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.CmsService = CmsService;
    this.ContentEditor = ContentEditor;
    this.DialogService = DialogService;
    this.ExtensionService = ExtensionService;
    this.OpenUiService = OpenUiService;
  }

  $onInit() {
    if (this.mdInputContainer) {
      this.mdInputContainer.setHasValue(true);
    }
  }

  $onChanges(changes) {
    if (changes.extensionId) {
      const extensionId = changes.extensionId.currentValue;
      this.extension = this.ExtensionService.getExtension(extensionId);

      this.createConnection(extensionId);
      this.setHeight('initial');

      this.connection.emitter.on('document.field.focus', () => this.$element.triggerHandler('focus'));
      this.connection.emitter.on('document.field.blur', () => this.$element.triggerHandler('blur'));
    }
  }

  $onDestroy() {
    this.destroyConnection();
  }

  async createConnection(extensionId) {
    this.destroyConnection();
    this.connection = this.OpenUiService.initialize(extensionId, {
      appendTo: this.$element[0],
      methods: {
        getDocument: this.getDocument.bind(this),
        getFieldValue: this.getValue.bind(this),
        openDocument: this.openDocument.bind(this),
        setFieldValue: this.setValue.bind(this),
        setFieldHeight: this.setHeight.bind(this),
      },
    });
    await this.connection.promise;
  }

  destroyConnection() {
    if (this.connection) {
      this.connection.emitter.clearListeners();
      this.connection.destroy();
    }
  }

  setValue(value) {
    value = `${value}`;
    if (value.length >= MAX_SIZE_IN_BYTES) {
      throw new Error(`Max value length of ${MAX_SIZE_IN_BYTES} bytes is reached.`);
    }

    this.ngModel.$setViewValue(value);
    this.value = value;
  }

  getValue() {
    return this.value;
  }

  setHeight(value) {
    let height = value === 'initial' ? this.extension.initialHeightInPixels : value;
    height = Math.max(MIN_HEIGHT_IN_PIXELS, Math.min(height, MAX_HEIGHT_IN_PIXELS));

    this.connection.iframe.style.height = `${height}px`;
  }

  getDocument() {
    const document = this.ContentEditor.getDocument();
    return {
      displayName: document.displayName,
      id: document.id,
      locale: document.info.locale,
      mode: 'edit',
      urlName: document.urlName,
      variant: {
        id: document.variantId,
      },
    };
  }

  async openDocument(id) {
    if (this.ContentEditor.isDocumentDirty()) {
      try {
        await this.ContentEditor.confirmClose('SAVE_CHANGES_TO_DOCUMENT');
      } catch (error) {
        throw new Error('Failed to close the unsaved document.');
      }
    }

    this.CmsService.publish('open-content', id, 'view');
  }
}
