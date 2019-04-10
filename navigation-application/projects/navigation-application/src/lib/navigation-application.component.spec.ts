import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NavigationApplicationComponent } from './navigation-application.component';

describe('NavigationApplicationComponent', () => {
  let component: NavigationApplicationComponent;
  let fixture: ComponentFixture<NavigationApplicationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NavigationApplicationComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NavigationApplicationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
