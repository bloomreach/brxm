import { TestBed } from '@angular/core/testing';

import { NavigationApplicationService } from './navigation-application.service';

describe('NavigationApplicationService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: NavigationApplicationService = TestBed.get(NavigationApplicationService);
    expect(service).toBeTruthy();
  });
});
