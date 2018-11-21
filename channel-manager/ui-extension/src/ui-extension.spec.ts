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
import { PageProperties, UiScope } from './api';
import UiExtension from './ui-extension';

jest.mock('./parent');

afterEach(() => {
  (connect as jest.Mock).mockClear();
});

describe('register', () => {
  it('connects to the parent API', () => {
    return UiExtension.register()
      .then(() => {
        expect(connect).toHaveBeenCalled();
      });
  });

  it('uses the parent origin provided as a URL search parameter', () => {
    window.history.pushState({}, 'Test Title', '/?br.parentOrigin=http%3A%2F%2Fcms.example.com%3A8080');

    return UiExtension.register()
      .then(() => {
        expect(connect).toHaveBeenCalledWith('http://cms.example.com:8080', expect.any(Object));
      });
  });

  describe('on success', () => {
    let ui: UiScope;

    beforeEach(() => UiExtension.register().then(api => (ui = api)));

    it('initializes the UI properties', () => {
      expect(ui.baseUrl).toBe('https://cms.example.com');
      expect(ui.extension.config).toBe('testConfig');
      expect(ui.locale).toBe('en');
      expect(ui.timeZone).toBe('Europe/Amsterdam');
      expect(ui.user).toBe('admin');
      expect(ui.version).toBe('13.0.0');
    });

    describe('ui.channel.refresh()', () => {
      it('refreshes the current channel', () => {
        return ui.channel.refresh().then(() => {
          expect(ParentConnection.call).toHaveBeenCalledWith('refreshChannel');
        });
      });
    });

    describe('ui.channel.page.get()', () => {
      it('returns the current page', () => {
        return ui.channel.page.get()
          .then((page) => {
            expect(page.channel.id).toBe('testChannelId');
            expect(page.id).toBe('testPageId');
            expect(page.sitemapItem.id).toBe('testSitemapItemId');
            expect(page.url).toBe('http://www.example.com');
          });
      });
    });

    describe('ui.channel.page.refresh()', () => {
      it('refreshes the current page', () => {
        return ui.channel.page.refresh().then(() => {
          expect(ParentConnection.call).toHaveBeenCalledWith('refreshPage');
        });
      });
    });

    describe('ui.channel.page.on(\'navigate\', listener)', () => {
      let nextPage: PageProperties;

      beforeEach(() => {
        nextPage = {
          channel: {
            id: 'channelId',
          },
          id: 'pageId',
          sitemapItem: {
            id: 'sitemapItemId',
          },
          url: 'http://www.example.com/page',
        };
      });

      it('calls the listener whenever the parent emits a \'channel.page.navigate\' event', () => {
        const eventEmitter = (connect as jest.Mock).mock.calls[0][1];
        const listener = jest.fn();

        ui.channel.page.on('navigate', listener);

        return eventEmitter.emit('channel.page.navigate', nextPage)
          .then(() => {
            expect(listener).toHaveBeenCalledWith(nextPage);
          });
      });

      it('returns an unbind function', () => {
        const eventEmitter = (connect as jest.Mock).mock.calls[0][1];
        const listener = jest.fn();

        const unbind = ui.channel.page.on('navigate', listener);
        unbind();

        return eventEmitter.emit('channel.page.navigate', nextPage)
          .then(() => {
            expect(listener).not.toHaveBeenCalled();
          });
      });
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
