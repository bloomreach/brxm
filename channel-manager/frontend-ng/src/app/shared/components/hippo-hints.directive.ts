import { Component, Directive, ElementRef, Input, OnChanges, OnInit, SimpleChange, SimpleChanges } from '@angular/core';

@Directive({
  selector: '[hippoHints]',
})
export class HippoHintsDirective implements OnInit, OnChanges {
  @Input() hippoHints: any;
  private initialized: boolean = false;
  private hintElements: any = {};
  private el: any;

  constructor(private elementRef: ElementRef) { }

  ngOnChanges(changes: SimpleChanges) {
    if(this.initialized) {
      const hints: SimpleChange = changes.hippoHints;
      this.processHintValues(hints.currentValue);
    }
  }

  processHintValues(values = null) {
    for(let key in this.hintElements) {
      if(values && values.hasOwnProperty(key)) {
        this.hintElements[key].el.style.display = '';
      } else {
        this.hintElements[key].el.style.display = 'none';
      }
    }
  }

  ngOnInit() {
    this.initialized = true;

    this.el = this.elementRef.nativeElement;
    const children = this.el.querySelectorAll('[hippohint]'); // hippoHint appears as hippohint in the DOM

    for(let i = 0; i < children.length; i++) {
      const child = children[i];
      if(!child.hasAttribute('hippohint')) continue;
      this.hintElements[child.getAttribute('hippohint')] = { el: child };
    }

    this.processHintValues(this.hippoHints);
  }
}
