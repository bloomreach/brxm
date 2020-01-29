/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { NGXLogger } from 'ngx-logger';

import { WindowRef } from '../shared/services/window-ref.service';

export const pendoFactory = (windowRef: WindowRef, logger: NGXLogger): pendo.Pendo => {
  const pendo = windowRef.nativeWindow.pendo;

  if (!pendo) {
    logger.warn('Pendo not found');
    return {} as any;
  }

  return pendo;
};
