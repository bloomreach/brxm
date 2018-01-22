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
    $translate,
    $log,
    CreateContentService,
    Step1Service,
    FeedbackService,
  ) {
    'ngInject';

    this.$translate = $translate;
    this.$log = $log;
    this.CreateContentService = CreateContentService;
    this.FeedbackService = FeedbackService;
    this.Step1Service = Step1Service;
  }

  getData() {
    return this.Step1Service.getData();
  }

  submit() {
    this.Step1Service.createDraft()
      .then(document => this.CreateContentService.next(document, this.getData()))
      .catch(error => this._onError(error, 'Unknown error creating new draft document'));
  }

  setLocale(locale) {
    this.Step1Service.setLocale(locale);
  }

  close() {
    this.CreateContentService.stop();
  }

  _onError(error, genericMessage) {
    if (error.data && error.data.reason) {
      const errorKey = this.$translate.instant(`ERROR_${error.data.reason}`);
      const args = [errorKey];
      if (error.data.params) args.push(error.data.params);
      this.FeedbackService.showError(...args);
    } else {
      this.$log.error(genericMessage, error);
    }
  }
}

export default Step1Controller;
