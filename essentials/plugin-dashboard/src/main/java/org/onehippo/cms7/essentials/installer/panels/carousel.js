$(function () {
    function setupSlide($slide) {
        $slide.find('.large').addClass('back')
                .find('img').css({
                    marginTop: 325,
                    marginLeft: 50,
                    width: 400
                });

        $slide.find('.small').addClass('back')
                .find('img').css({
                    marginTop: 325,
                    marginRight: 50,
                    width: 300
                });
    }

    function slideIn($slide) {
        $slide.find('.large img')
                .animate({
                    width: 450,
                    marginLeft: 0
                }, {
                    queue: false,
                    duration: _duration * 1.5
                })
                .animate({
                    marginTop: -25
                }, {
                    duration: _duration,
                    complete: function () {
                        $slide.find('.large').removeClass('back');
                    }
                })
                .animate({
                    marginTop: 50
                }, {
                    duration: _duration / 2
                });

        $slide.find('.small img')
                .delay(_duration)
                .animate({
                    marginTop: 50
                }, {
                    duration: _duration,
                    complete: function () {
                        $slide.find('.small').removeClass('back');
                    }
                })
                .animate({
                    marginTop: 100
                }, {
                    duration: _duration / 2
                });
        setTimeout(function () {
            $slide.find('.small img')
                    .animate({
                        width: 350,
                        marginRight: 0
                    }, {
                        queue: false,
                        duration: _duration
                    });
        }, _duration * 1.5);
    }

    function slideOut($slide) {
        $slide.find('.small img')
                .animate({
                    width: 300,
                    marginRight: 50
                }, {
                    queue: false,
                    duration: _duration * 1.5
                })
                .animate({
                    marginTop: 50
                }, {
                    duration: _duration / 2,
                    complete: function () {
                        $slide.find('.small').addClass('back');
                    }
                })
                .animate({
                    marginTop: 325
                }, {
                    duration: _duration
                });

        $slide.find('.large img')
                .delay(_duration / 2)
                .animate({
                    marginTop: -25
                }, {
                    duration: _duration / 2,
                    complete: function () {
                        $slide.find('.large').addClass('back');
                    }
                })
                .animate({
                    marginTop: 325
                }, {
                    duration: _duration
                });

        setTimeout(function () {
            $slide.find('.large img')
                    .animate({
                        width: 400,
                        marginLeft: 50
                    }, {
                        queue: false,
                        duration: _duration
                    });
        }, _duration / 2);
    }

    var $carousel = $('#carousel');
    var _duration = 500;

    $carousel.carouFredSel({
        items: 1,
        pagination: '#pager',
        scroll: {
            fx: 'none',
            timeoutDuration: 4000,
            conditions: function (direction) {
                if ($carousel.hasClass('prepared')) {
                    $carousel.removeClass('prepared');
                    return true;
                }
                if ($carousel.hasClass('animating')) {
                    return false;
                }
                $carousel.addClass('animating');

                var $slide = $carousel.children().first();

                slideOut($slide);
                setTimeout(
                        function () {
                            $carousel.addClass('prepared');
                            $carousel.trigger(direction);
                        }, _duration * 2
                );

                return false;
            },
            onAfter: function (data) {
                setupSlide(data.items.visible);
                slideIn(data.items.visible);
                setTimeout(
                        function () {
                            $carousel.removeClass('animating');
                        }, _duration * 2.5
                );
            }
        }
    });
});