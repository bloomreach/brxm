import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { IframeAppComponent } from './iframe-app.component';

describe('IframeAppComponent', () => {
  let component: IframeAppComponent;
  let fixture: ComponentFixture<IframeAppComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ IframeAppComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IframeAppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
