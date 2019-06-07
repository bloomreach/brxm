import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ToolbarDrawerComponent } from './toolbar-drawer.component';

describe('ToolbarDrawerComponent', () => {
  let component: ToolbarDrawerComponent;
  let fixture: ComponentFixture<ToolbarDrawerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ToolbarDrawerComponent ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ToolbarDrawerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
