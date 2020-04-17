/*******************************************************************************
 * Copyright (c) 2012, 2020 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package com.vmware.vfabric.ide.eclipse.tcserver.core.util;

import java.io.File;
import java.io.IOException;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @since 2.5.2
 */
public interface ProcessRunner {

	int run(File workingDir, String... command) throws IOException, InterruptedException;

}
