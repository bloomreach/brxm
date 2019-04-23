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

  constructor(parameters) {
    this.MIN_HEIGHT_IN_PIXELS = 10;
    this.MAX_HEIGHT_IN_PIXELS = 10000;
    this.MAX_SIZE_IN_BYTES = 102400;

    this.parameters = parameters;
    this.element = new AutoSaveElement(parameters.hiddenValueId, parameters.autoSaveUrl, parameters.autoSaveDelay);
  }

  onConnect(connection) {
    this.iframe = connection.iframe;
    this.setFieldHeight(this.parameters.initialHeightInPixels);

    connection.emitter.on('document.field.blur', () => this.element.blur());
  }

  onDestroy() {
    this.element.clearScheduledSave();
  }

  getMethods() {
    return {
      getDocument: this.getDocumentProperties.bind(this),
      getFieldValue: this.getFieldValue.bind(this),
      setFieldValue: this.setFieldValue.bind(this),
      getFieldCompareValue: this.getFieldCompareValue.bind(this),
      setFieldHeight: this.setFieldHeight.bind(this),
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
    return this.element.getValue();
  }

  setFieldValue(value) {
    if (value.length >= this.MAX_SIZE_IN_BYTES) {
      throw new Error('Max value length of ' + this.MAX_SIZE_IN_BYTES + ' is reached.');
    }
    this.element.setValue(value);
  }

  getFieldCompareValue() {
    return this.parameters.compareValue;
  }

  setFieldHeight(pixels) {
    const height = Math.max(this.MIN_HEIGHT_IN_PIXELS, Math.min(pixels, this.MAX_HEIGHT_IN_PIXELS));
    this.iframe.style.height = height + 'px';
  }
}

OpenUi.registerClass(OpenUiStringPlugin);

class AutoSaveElement {

  constructor(id, url, delay) {
    this.element = document.getElementById(id);
    this.url = url;
    this.delay = delay;

    this.savePending = false;
    this.saveTimeout = null;
  }

  setValue(value) {
    this.element.value = value;

    if (this.savePending || this.saveTimeout) {
      this.scheduleSave();
    } else {
      this.save();
    }
  }

  getValue() {
    return this.element.value;
  }

  save() {
    this.savePending = true;
    Wicket.Ajax.post({
      u: this.url,
      ep: {
        data: this.element.value
      },
      coh: [() => this.savePending = false],
    });
  }

  scheduleSave() {
    this.clearScheduledSave();
    this.saveTimeout = setTimeout(() => {
      this.saveTimeout = null;
      this.save();
    }, this.delay);
  }

  clearScheduledSave() {
    clearTimeout(this.saveTimeout);
    this.saveTimeout = null;
  }

  blur() {
    if (this.saveTimeout) {
      this.clearScheduledSave();
      this.save();
    }
  }
}
