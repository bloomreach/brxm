/*
 *  Copyright 2008-2023 Bloomreach
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.login;

import java.util.StringTokenizer;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated since 15.2.3. Server side browser detection is no longer supported and will be replaced by client side
 * browser detection in the future.
 */
@Deprecated
public class BrowserCheckBehavior extends Behavior {

}
