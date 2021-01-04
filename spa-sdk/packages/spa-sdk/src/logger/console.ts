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

import { inject, injectable } from 'inversify';
import { Level } from './level';
import { Logger } from './logger';

export const ConsoleToken = Symbol.for('ConsoleToken');

@injectable()
export class ConsoleLogger extends Logger {
  constructor(@inject(ConsoleToken) private console: Console) {
    super();
  }

  protected write(level: Level, ...message: unknown[]): void {
    this.console[level](...message);
  }
}
