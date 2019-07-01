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

// tslint:disable-next-line:no-reference
/// <reference path="../../../node_modules/@types/jasmine/index.d.ts" />

type SpyObjMethodNames = ReadonlyArray<string> | { [methodName: string]: any };
interface SpyObjGetters {
  [getterName: string]: any;
}

export const createSpyObj = (methodNames: SpyObjMethodNames, getters: SpyObjGetters = {}) => {
  if (Array.isArray(methodNames) && methodNames.length === 0) {
    methodNames.push('_');
  }

  const obj = jasmine.createSpyObj(methodNames);

  Object.keys(getters).forEach(key => {
    const value = getters[key];

    Object.defineProperty(obj, key, {
      get: () => undefined,
    });
    spyOnProperty(obj, key, 'get').and.returnValue(value);
  });

  return obj;
};
