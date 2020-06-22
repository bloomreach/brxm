/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

export class AppError extends Error {
  constructor(
    public code: number,
    message: string,
    public description?: string,
    public internalDescription?: string,
  ) {
    super(message);

    Object.setPrototypeOf(this, AppError.prototype);
    this.stack = this.getStack();
    this.name = 'AppError';
  }

  toString(): string {
    return `${this.name}: ${this.message}: ${this.description}: ${this.internalDescription}`;
  }

  protected getStack(): string {
    const stack = (new Error()).stack.split('\n');

    // Skip internal stack entries
    return stack.slice(3).join('\n');
  }
}
