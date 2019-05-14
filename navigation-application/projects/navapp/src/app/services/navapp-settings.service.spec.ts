import { TestBed } from '@angular/core/testing';

import { NavAppSettingsService } from './navapp-settings.service';

describe('BrxGlobalService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: NavAppSettingsService = TestBed.get(NavAppSettingsService);
    expect(service).toBeTruthy();
  });
});
