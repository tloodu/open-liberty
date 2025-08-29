/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                CommonWebServerTests40.class,
                ErrorPage40Test.class,
                CDITests40.class,
                ConfigDropinRootTests40.class,
                ConfigSpringBootApplicationClassloaderTests40.class,
                ConfigSpringBootApplicationTagTests40.class,
                ConfigSpringBootApplicationWithArgsTests40.class,
                EnableSpringBootTraceTests40.class,
                ExceptionOccuredAfterAppIsAvailableTest40.class,
                MissingServletTests40.class

})

public class FATSuite {
}
