/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jca.fat.classloading.cdilib;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * A CDI extension in a shared library that observes CDI lifecycle events.
 */
public class CDIExtension implements Extension {

    public static final String MESSAGE = "CDI Extension from Library";
    private static boolean extensionLoaded = false;

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        System.out.println("beforeBeanDiscovery called");
        extensionLoaded = true;
    }

    public static String getMessage() {
        return MESSAGE;
    }

    public static boolean isExtensionLoaded() {
        return extensionLoaded;
    }
}
