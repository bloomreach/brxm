import {
  Directive,
  OnInit,
  ElementRef,
  Input,
  OnChanges,
  SimpleChange,
  SimpleChanges
} from '@angular/core';

@Directive({
  selector: '[formHints]',
})
export class FormHintsDirective implements OnInit, OnChanges {
  @Input() formHints: any;

  private initialized = false;
  private hintElements: any = {};
  private el: any;

  constructor(private elementRef: ElementRef) { }

  ngOnInit() {
    this.initialized = true;

    this.el = this.elementRef.nativeElement;
    const children = this.el.querySelectorAll('[key]');

    for (let i = 0; i < children.length; i++) {
      const child = children[i];
      if (child.hasAttribute('key')) {
        this.hintElements[child.getAttribute('key')] = { el: child };
      }
    }

    this.processHintValues(this.formHints);
  }

  processHintValues(values) {
    if (!values) values = {};

    for (const key in this.hintElements) {
      this.hintElements[key].el.style.display = (values.hasOwnProperty(key) && values[key] === true) ? '' : 'none';
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.initialized) {
      const hints: SimpleChange = changes.formHints;
      this.processHintValues(hints.currentValue);
    }
  }
}
