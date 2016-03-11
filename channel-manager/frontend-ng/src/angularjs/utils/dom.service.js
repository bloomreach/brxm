/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class DomService {

  constructor($q) {
    'ngInject';

    this.$q = $q;
  }

  getAppRootUrl() {
    const appPath = document.location.pathname.substring(0, document.location.pathname.length - 'index.html'.length);
    return `//${document.location.host}${appPath}`;
  }

  addCss(window, href) {
    const link = window.document.createElement('link');
    link.type = 'text/css';
    link.rel = 'stylesheet';
    link.href = href;
    window.document.head.appendChild(link);
  }

  addScript(window, src) {
    return this.$q(function (resolve, reject) {
      const script = window.document.createElement('script');
      script.type = 'text/javascript';
      script.src = src;
      script.addEventListener('load', resolve);
      script.addEventListener('error', reject);
      window.document.body.appendChild(script);
    });
  }

}
