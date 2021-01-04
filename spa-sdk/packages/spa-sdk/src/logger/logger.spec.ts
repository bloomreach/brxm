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

import { Level } from './level';
import { Logger } from './logger';

describe('Logger', () => {
  const logger = new class extends Logger {
    level = Level.Warn;
    write = jest.fn();
  }();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('log', () => {
    it('should not log messages below log level', () => {
      logger.debug('something');
      logger.info('something');

      expect(logger.write).not.toBeCalled();
    });

    it('should log messages above log level', () => {
      logger.warn('something');
      logger.error('something');

      expect(logger.write).toBeCalledTimes(2);
    });

    it('should log a message', () => {
      logger.warn('a', 'b');

      expect(logger.write).toBeCalledWith('warn', expect.any(String), '[WARN]', 'a', 'b');
    });
  });
});
