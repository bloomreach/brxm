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

import { injectable } from 'inversify';
import { Level } from './level';

export interface Logger {
  level: Level;
  debug(...message: unknown[]): void;
  info(...message: unknown[]): void;
  warn(...message: unknown[]): void;
  error(...message: unknown[]): void;
}

@injectable()
export abstract class Logger implements Logger {
  level = Level.Error;

  constructor() {
    this.debug = this.log.bind(this, Level.Debug);
    this.info = this.log.bind(this, Level.Info);
    this.warn = this.log.bind(this, Level.Warn);
    this.error = this.log.bind(this, Level.Error);
  }

  private log(level: Level, ...message: unknown[]) {
    const levels = Object.values(Level);
    if (!levels.includes(level) || levels.indexOf(level) < levels.indexOf(this.level)) {
      return;
    }

    return this.write(level, '[SPA]', `[${level.toUpperCase()}]`, ...message);
  }

  protected abstract write(level: Level, ...message: unknown[]): void;
}
