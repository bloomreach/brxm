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
  static register(onSuccess: (ui: Ui) => void) {
    if (typeof onSuccess !== 'function') {
      throw new Error('No callback function provided');
    }

    const connection = UiExtension.connectToParent();
    if (connection) {
      connection.promise.then((parent: Parent) => {
        try {
          parent.getProperties()
            .then(onSuccess)
            .catch((e) => {
              console.error('Failed to register extension, cannot get parent properties:', e);
            })
        } catch (e) {
          console.error('Failed to register extension: cannot get parent properties. '
            + 'Are you using compatible versions of BloomReach Experience and the client library?');
        }
      });
    }
  }

  private static connectToParent(): Penpal.IConnectionObject {
    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    try {
      return Penpal.connectToParent({
        parentOrigin,
      });
    } catch (e) {
      console.info('Failed to register extension: cannot connect to parent');
    }
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
