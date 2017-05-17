class resizeHandleController {
  constructor($element, $document) {
    'ngInject';

    this.$document = $document;
    this.handle = $element;
  }

  $onInit() {
    this._registerEvents(this.element);
  }

  _registerEvents() {
    this.handle.mousedown((mouseDownEvent) => {
      let newWidth;
      const hippoIframe = $('hippo-iframe').find('iframe');
      hippoIframe.css('pointer-events', 'none');
      const initialWidth = this.element.width();
      const initialX = mouseDownEvent.clientX;

      this.$document.mousemove((moveEvent) => {
        const diff = initialX - moveEvent.pageX;
        newWidth = initialWidth + diff;

        if (newWidth < 440 || newWidth > 880) return;

        this.element.css('width', newWidth);
        this.element.css('max-width', newWidth);
      });

      this.$document.mouseup(() => {
        this.$document.unbind('mousemove');
        hippoIframe.css('pointer-events', 'auto');
        this.onResize({ newWidth });
      });
    });
  }
}

export default resizeHandleController;
