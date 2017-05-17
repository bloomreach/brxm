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
      const hippoIframe = $('hippo-iframe').find('iframe');
      const initialWidth = this.element.width();
      const initialX = mouseDownEvent.clientX;

      hippoIframe.css('pointer-events', 'none');

      let newWidth;

      this.$document.mousemove((moveEvent) => {
        const diff = initialX - moveEvent.pageX;

        newWidth = initialWidth + diff;

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
