/*
 * Copyright 2021 Bloomreach Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SvgValidator {

    /**
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/SVG/Element">
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element
     * </a>
     */
    private static final Set<String> SVG_ELEMENTS = Stream.of("svg", "altglyph", "altglyphdef", "altglyphitem",
            "animatecolor", "animatemotion", "animatetransform", "circle", "clippath", "defs", "desc", "ellipse",
            "filter", "font", "g", "glyph", "glyphref", "hkern", "image", "line", "lineargradient", "marker", "mask",
            "metadata", "mpath", "path", "pattern", "polygon", "polyline", "radialgradient", "rect", "stop", "style",
            "switch", "symbol", "text", "textpath", "title", "tref", "tspan", "use", "view", "vkern", "feBlend",
            "feColorMatrix", "feComponentTransfer", "feComposite", "feConvolveMatrix", "feDiffuseLighting",
            "feDisplacementMap", "feDistantLight", "feFlood", "feFuncA", "feFuncB", "feFuncG", "feFuncR",
            "feGaussianBlur", "feMerge", "feMergeNode", "feMorphology", "feOffset", "fePointLight",
            "feSpecularLighting", "feSpotLight", "feTile", "feTurbulence").collect(Collectors.toSet());
    /**
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute">
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute
     * </a>
     */

    private static final Set<String> SVG_ATTRIBUTES = Stream.of("accent-height", "accumulate", "additivive",
            "alignment-baseline", "ascent", "attributename", "attributetype", "azimuth", "baseprofile", "basefrequency",
            "baseline-shift", "begin", "bias", "by", "class", "clip", "clip-path", "clip-rule", "color",
            "color-interpolation", "color-interpolation-filters", "color-profile", "color-rendering", "cx", "cy", "d",
            "dx", "dy", "diffuseconstant", "direction", "display", "divisor", "dur", "edgemode", "elevation", "end",
            "fill", "fill-opacity", "fill-rule", "filter", "flood-color", "flood-opacity", "font-family", "font-size",
            "font-size-adjust", "font-stretch", "font-style", "font-variant", "font-weight", "fx", "fy", "g1", "g2",
            "glyph-name", "glyphref", "gradientunits", "gradienttransform", "height", "href", "id", "image-rendering",
            "in", "in2", "k", "k1", "k2", "k3", "k4", "kerning", "keypoints", "keysplines", "keytimes", "lang",
            "lengthadjust", "letter-spacing", "kernelmatrix", "kernelunitlength", "lighting-color", "local",
            "marker-end", "marker-mid", "marker-start", "markerheight", "markerunits", "markerwidth",
            "maskcontentunits", "maskunits", "max", "mask", "media", "method", "mode", "min", "name", "numoctaves",
            "offset", "operator", "opacity", "order", "orient", "orientation", "origin", "overflow", "paint-order",
            "path", "pathlength", "patterncontentunits", "patterntransform", "patternunits", "points", "preservealpha",
            "preserveaspectratio", "r", "rx", "ry", "radius", "refx", "refy", "repeatcount", "repeatdur", "restart",
            "result", "rotate", "scale", "seed", "shape-rendering", "specularconstant", "specularexponent",
            "spreadmethod", "stddeviation", "stitchtiles", "stop-color", "stop-opacity", "stroke-dasharray",
            "stroke-dashoffset", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "stroke-opacity", "stroke",
            "stroke-width", "style", "surfacescale", "tabindex", "targetx", "targety", "transform", "text-anchor",
            "text-decoration", "text-rendering", "textlength", "type", "u1", "u2", "unicode", "version", "values",
            "viewbox", "visibility", "vert-adv-y", "vert-origin-x", "vert-origin-y", "width", "word-spacing", "wrap",
            "writing-mode", "xchannelselector", "ychannelselector", "x", "x1", "x2", "xlink:href", "xmlns", "xml:space",
            "xmlns:xlink", "y", "y1", "y2", "z", "zoomandpan").collect(Collectors.toSet());

    private SvgValidator() {
    }


    public static SvgValidationResult validate(final InputStream is) throws
            ParserConfigurationException,
            SAXException,
            IOException {

        SvgValidationResult.SvgValidationResultBuilder builder = SvgValidationResult.builder();
        DefaultHandler handler = new DefaultHandler() {

            private boolean inStyleElement;
            private String styleContents = StringUtils.EMPTY;

            @Override
            public void startElement(final String uri, final String localName, final String qName,
                                     final Attributes attributes) {
                if ("style".equals(qName)){
                    inStyleElement = true;
                }
                if (!SVG_ELEMENTS.contains(qName)) {
                    builder.offendingElement(qName);
                }
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attributeName = attributes.getQName(i);
                    if (!SVG_ATTRIBUTES.contains(attributeName)) {
                        builder.offendingAttribute(attributeName);
                    }
                }
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) throws
                    SAXException {
                if (inStyleElement) {
                    inStyleElement = false;
                    styleContents = StringUtils.EMPTY;

                }
            }

            @Override
            public void characters(final char[] ch, final int start, final int length) throws
                    SAXException {
               if (inStyleElement){
                   if (StringUtils.containsIgnoreCase(String.valueOf(ch),"javascript")){
                       throw new SAXException("Javascript inside style element is not supported");
                   }
               }
            }
        };

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        SAXParser parser = factory.newSAXParser();
        parser.parse(is, handler);
        return builder.build();
    }
}
