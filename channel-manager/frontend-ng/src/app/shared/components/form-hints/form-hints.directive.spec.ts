import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormHintsDirective } from './form-hints.directive';
import { By } from '@angular/platform-browser';

/*
  Test Wrapping Component
 */
@Component({
  template: `
    <div [formHints]="testErrors" class="hintsElement">
      <div id="requiredHint" key="required">I am required error</div>
      <div id="minlengthHint" key="minlength">I am minlength error</div>
    </div>
  `
})
class TestFormHintsComponent {
  testErrors: Object = {
    required: false,
    minlength: false
  }
}

/*
  Test Scenarios
 */
describe('form-hints directive', () => {
  let component: TestFormHintsComponent;
  let fixture: ComponentFixture<TestFormHintsComponent>;
  let hintsElement: DebugElement;
  let directive: any;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestFormHintsComponent, FormHintsDirective]
    });
    fixture = TestBed.createComponent(TestFormHintsComponent);
    component = fixture.componentInstance;
    hintsElement = fixture.debugElement.query(By.directive(FormHintsDirective));
    directive = hintsElement.injector.get(FormHintsDirective);
  });

  /*
    Helper function for directive initialization
   */
  function initialize () {
    spyOn(directive, 'processHintValues');

    directive.ngOnInit();
    fixture.detectChanges();
  }

  function DOMDelay(callback) {
    setTimeout(() => callback());
  }

  it('initializes formHints directive (ngOnInit)', () => {
    expect(directive.initialized).toBe(false);
    initialize();

    expect(directive.initialized).toBe(true);
    expect(directive.el).toBeDefined();
    expect(directive.hintElements.required).toBeDefined();
    expect(directive.hintElements.minlength).toBeDefined();
    expect(directive.processHintValues).toHaveBeenCalledWith({
      required: false,
      minlength: false
    });

    fixture.detectChanges();
    DOMDelay(() => {
      expect(directive.hintElements.required.el.style.display).toEqual('none');
      expect(directive.hintElements.minlength.el.style.display).toEqual('none');
    });
  });

  it('detects changes in formHints and processes them (processHintValues)', () => {
    initialize();

    // Change values in wrapping component
    component.testErrors['required'] = true;

    // Changed values should be reflected in formHints directive
    expect(directive.processHintValues).toHaveBeenCalledWith({
      required: true,
      minlength: false
    });

    fixture.detectChanges();
    // Expect only "required" hint to be displayed
    DOMDelay(() => {
      expect(directive.hintElements.required.el.style.display).toEqual('');
      expect(directive.hintElements.minlength.el.style.display).toEqual('none');
    });
  });

  it('processes empty values (processHintValues)', () => {
    initialize();

    expect(() => { directive.processHintValues(null); }).not.toThrow();

    component.testErrors = null;
    expect(directive.processHintValues).toHaveBeenCalledWith(undefined);

    fixture.detectChanges();
    // Expect all hints to be hidden if value is invalid
    DOMDelay(() => {
      expect(directive.hintElements.required.el.style.display).toEqual('none');
      expect(directive.hintElements.minlength.el.style.display).toEqual('none');
    });
  });
});
