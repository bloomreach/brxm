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

import { connect, ParentConnection } from './parent';
import { Ui } from './ui';
import UiExtension from './ui-extension';

jest.mock('./parent');

afterEach(() => {
  (connect as jest.Mock).mockClear();
});

describe('UiExtension.register()', () => {
  it('connects to the parent API', () =>
    UiExtension.register()
      .then(() => {
        expect(connect).toHaveBeenCalled();
      }),
  );

  it('uses the parent origin provided as a URL search parameter', () => {
    window.history.pushState({}, 'Test Title', '/?br.parentOrigin=http%3A%2F%2Fcms.example.com%3A8080');

    return UiExtension.register()
      .then(() => {
        expect(connect).toHaveBeenCalledWith('http://cms.example.com:8080', expect.any(Object));
      });
  });

  describe('on success', () => {
    let ui: Ui;

    beforeEach(() => UiExtension.register().then(api => (ui = (api as Ui))));

    it('resolves with an initialized UI object', () => {
      expect(ui.baseUrl).toBe('https://cms.example.com');
      expect(ui.channel).toBeDefined();
      expect(ui.extension.config).toBe('testConfig');
      expect(ui.locale).toBe('en');
      expect(ui.timeZone).toBe('Europe/Amsterdam');
      expect(ui.user.id).toBe('admin');
      expect(ui.user.firstName).toBe('Ad');
      expect(ui.user.lastName).toBe('Min');
      expect(ui.user.displayName).toBe('Ad Min');
      expect(ui.version).toBe('13.0.0');
    });
  });

  describe('on failure', () => {
    it('rejects with the error returned by parent.connect()', () => {
      const error = new Error('error');

      (connect as jest.Mock).mockImplementation(() => {
        return Promise.reject(error);
      });

      return expect(UiExtension.register()).rejects.toBe(error);
    });

    it('rejects with an error when calling a parent method fails', () => {
      (ParentConnection.call as jest.Mock).mockImplementation(() => {
        throw new Error('error');
      });

      return expect(UiExtension.register()).rejects.toBeInstanceOf(Error);
    });
  });
});
