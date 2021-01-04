/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { ConsoleLogger } from './console';

describe('ConsoleLogger', () => {
  let console: jest.Mocked<Console>;
  let logger: ConsoleLogger;

  beforeEach(() => {
    console = { error: jest.fn() } as unknown as typeof console;
    logger = new ConsoleLogger(console);
  });

  describe('write', () => {
    it('should use the console to log a message', () => {
      logger.error('something');

      expect(console.error).toBeCalledWith(expect.any(String), expect.any(String), 'something');
    });
  });
});
