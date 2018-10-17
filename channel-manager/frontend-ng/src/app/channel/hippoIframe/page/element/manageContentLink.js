/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import EmbeddedLink from './embeddedLink';

class ManageContentLink extends EmbeddedLink {
  constructor(commentElement, metaData) {
    super('manage-content-link', commentElement, metaData);
  }

  getDefaultPath() {
    return this.metaData.defaultPath;
  }

  getParameterName() {
    return this.metaData.parameterName;
  }

  getParameterValue() {
    return this.metaData.parameterValue;
  }

  isParameterValueRelativePath() {
    return this.metaData.parameterValueIsRelativePath === 'true';
  }

  getPickerConfig() {
    if (!this.metaData.parameterName) {
      return null;
    }
    return {
      configuration: this.metaData.pickerConfiguration,
      initialPath: this.metaData.pickerInitialPath,
      isRelativePath: false, // the path is made relative in HstComponentService#saveParameter, and not by the picker
      remembersLastVisited: this.metaData.pickerRemembersLastVisited === 'true',
      rootPath: this.metaData.pickerRootPath,
      selectableNodeTypes: this.metaData.pickerSelectableNodeTypes ?
        this.metaData.pickerSelectableNodeTypes.split(',') : [],
    };
  }

  getRootPath() {
    return this.metaData.rootPath;
  }

  getDocumentTemplateQuery() {
    return this.metaData.documentTemplateQuery;
  }
}

export default ManageContentLink;
