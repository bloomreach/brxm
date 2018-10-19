/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Penpal from 'penpal';

export interface Parent {
  getProperties: () => Promise<Ui>,
}

export interface Ui {
  user: string,
}

export default class UiExtension {
  static register(onReady: (ui: Ui) => void) {
    if (typeof onReady !== 'function') {
      throw new Error('No callback function provided');
    }

    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    const connection = Penpal.connectToParent({
      parentOrigin,
    });

    connection.promise.then((parent: Parent) => {
      parent.getProperties().then(onReady);
    });
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
