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
import UiExtension from './ui-extension';

jest.mock('penpal');

describe('register', () => {
  it('connects to the parent API', () => {
    UiExtension.register(() => {
      expect(Penpal.connectToParent).toHaveBeenCalled();
    });
  });

  it('uses the parent origin provided as a URL search parameter', () => {
    window.history.pushState({}, 'Test Title', '/?br.parentOrigin=http%3A%2F%2Fcms.example.com%3A8080');

    UiExtension.register(() => {
      expect(Penpal.connectToParent).toHaveBeenCalledWith({
        parentOrigin: 'http://cms.example.com:8080',
      });
    });
  });

  it('throws an error if no callback is provided', () => {
    expect(UiExtension.register).toThrow(Error);
  });

  it('provides the UI properties to the callback', () => {
    UiExtension.register((ui) => {
      expect(ui.user).toBe('admin');
    });
  });
});
