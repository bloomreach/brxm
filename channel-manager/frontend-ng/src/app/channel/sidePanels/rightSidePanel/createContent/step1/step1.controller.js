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

class Step1Controller {
  constructor(
    CreateContentService,
    Step1Service,
    CmsService,
  ) {
    'ngInject';

    this.CreateContentService = CreateContentService;
    this.Step1Service = Step1Service;
    this.CmsService = CmsService;
  }

  get defaultPath() {
    return this.Step1Service.defaultPath;
  }

  set defaultPath(defaultPath) {
    this.Step1Service.defaultPath = defaultPath;
  }

  get documentType() {
    return this.Step1Service.documentType;
  }

  set documentType(documentType) {
    this.Step1Service.documentType = documentType;
  }

  get documentTypes() {
    return this.Step1Service.documentTypes;
  }

  get locale() {
    return this.Step1Service.locale;
  }

  set locale(locale) {
    this.Step1Service.locale = locale;
  }

  get name() {
    return this.Step1Service.name;
  }

  set name(name) {
    this.Step1Service.name = name;
  }

  get rootPath() {
    return this.Step1Service.rootPath;
  }

  get url() {
    return this.Step1Service.url;
  }

  set url(url) {
    this.Step1Service.url = url;
  }

  submit() {
    this.Step1Service.createDraft()
      .then(document => this.CreateContentService.next(document, this.url, this.locale));
    this.CmsService.reportUsageStatistic('CreateContent1Create');
  }

  close() {
    this.CreateContentService.stop();
    this.CmsService.reportUsageStatistic('CreateContent1Cancel');
  }
}

export default Step1Controller;
