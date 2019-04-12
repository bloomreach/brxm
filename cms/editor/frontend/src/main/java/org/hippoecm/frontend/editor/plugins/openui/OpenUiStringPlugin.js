/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class OpenUiStringPlugin {

  MIN_HEIGHT_IN_PIXELS = 10;
  MAX_HEIGHT_IN_PIXELS = 10000;
  MAX_SIZE_IN_BYTES = 102400;

  constructor(parameters) {
    this.parameters = parameters;
    this.hiddenValueElement = document.getElementById(parameters.hiddenValueId);
    this.scheduledSave = null;
    this.dialog = {};
  }

  onConnect(connection) {
    this.iframe = connection.iframe;
    this.setFieldHeight(this.parameters.initialHeightInPixels);
  }

  onDestroy() {
    clearTimeout(this.scheduledSave);
  }

  getMethods() {
    return {
      getDocument: this.getDocumentProperties.bind(this),
      getFieldValue: this.getFieldValue.bind(this),
      setFieldValue: this.setFieldValue.bind(this),
      getFieldCompareValue: this.getFieldCompareValue.bind(this),
      setFieldHeight: this.setFieldHeight.bind(this),
      openDialog: this.openDialog.bind(this),
    }
  }

  getDocumentProperties() {
    return {
      displayName: this.parameters.documentDisplayName,
      id: this.parameters.documentId,
      locale: this.parameters.documentLocale,
      mode: this.parameters.documentEditorMode,
      urlName: this.parameters.documentUrlName,
      variant: {
        id: this.parameters.documentVariantId
      }
    }
  }

  getFieldValue() {
    return this.hiddenValueElement.value;
  }

  setFieldValue(value) {
    if (value.length >= this.MAX_SIZE_IN_BYTES) {
      throw new Error('Max value length of ' + this.MAX_SIZE_IN_BYTES + ' is reached.');
    }
    this.hiddenValueElement.value = value;
    this.scheduleSave(value);
  }

  getFieldCompareValue() {
    return this.parameters.compareValue;
  }

  setFieldHeight(pixels) {
    const height = Math.max(this.MIN_HEIGHT_IN_PIXELS, Math.min(pixels, this.MAX_HEIGHT_IN_PIXELS));
    this.iframe.style.height = height + 'px';
  }

  openDialog() {
    return new Promise((resolve, reject) => {
      this.dialog = {
        resolve,
        reject,
      };

      Wicket.Ajax.get({
        u: this.parameters.dialogUrl,
        fh: (e) => reject(e),
      });

    });
  }

  closeDialog(result) {
    this.dialog.resolve(result);
  }

  cancelDialog() {
    this.dialog.reject({ code: 'DialogCanceled' });
  }

  scheduleSave(data) {
    clearTimeout(this.scheduledSave);
    this.scheduledSave = setTimeout(() => this.save(data), this.parameters.autoSaveDelay);
  }

  save(data) {
    Wicket.Ajax.post({
      u: this.parameters.autoSaveUrl,
      ep: {
        data: data
      }
    });
  }
}

OpenUi.registerClass(OpenUiStringPlugin);
