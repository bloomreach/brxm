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

import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { AuthService } from '../services/auth.service';
import { NavConfigService } from '../services/nav-config.service';
import { NavItemService } from '../services/nav-item.service';

import { BootstrapService } from './bootstrap.service';

export const appInitializer = (
  authService: AuthService,
  navConfigService: NavConfigService,
  navItemService: NavItemService,
  bootstrapService: BootstrapService,
  errorHandlingService: ErrorHandlingService,
) => async () => {
  try {
    await authService.loginAllResources();
    const navItemDtos = await navConfigService.init();
    const navItems = navItemService.registerNavItemDtos(navItemDtos);

    bootstrapService.bootstrap(navItems);
  } catch (error) {
    errorHandlingService.setCriticalError('ERROR_UNABLE_TO_LOAD_CONFIGURATION', error.message);
  }
};
