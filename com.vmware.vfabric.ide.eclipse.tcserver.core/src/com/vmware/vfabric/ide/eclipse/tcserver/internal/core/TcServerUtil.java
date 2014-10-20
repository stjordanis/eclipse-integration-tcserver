/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package com.vmware.vfabric.ide.eclipse.tcserver.internal.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.springsource.ide.eclipse.commons.core.StatusHandler;

/**
 * @author Steffen Pingel
 * @author Tomasz Zarna
 */
public class TcServerUtil {

	public static final String TEMPLATES_FOLDER = "templates";

	private static final String TEMPLATE_VARIATION_STR = "-tomcat-";

	@Deprecated
	public static boolean isSpringSource(IRuntimeWorkingCopy wc) {
		return wc != null && wc.getRuntimeType() != null && wc.getRuntimeType().getId().startsWith("com.springsource");
	}

	@Deprecated
	public static boolean isVMWare(IRuntimeWorkingCopy wc) {
		return wc != null && wc.getRuntimeType() != null && wc.getRuntimeType().getId().startsWith("com.vmware");
	}

	public static String getServerVersion(IRuntime runtime) {
		// String directory = ((Runtime)
		// runtime).getAttribute(TcServerRuntime.KEY_SERVER_VERSION, (String)
		// null);
		String directory = TcServerRuntime.getTomcatLocation(runtime).lastSegment();
		return getServerVersion(directory);
	}

	public static String getServerVersion(String tomcatFolerName) {
		return (tomcatFolerName != null && tomcatFolerName.startsWith("tomcat-")) ? tomcatFolerName.substring(7)
				: tomcatFolerName;
	}

	public static void importRuntimeConfiguration(IServerWorkingCopy wc, IProgressMonitor monitor) throws CoreException {
		// invoke tc Server API directly since
		// TcServer.importRuntimeConfiguration() swallows exceptions
		((TcServer) ((ServerWorkingCopy) wc).getWorkingCopyDelegate(monitor)).importRuntimeConfigurationChecked(
				wc.getRuntime(), monitor);
	}

	public static IStatus validateInstance(File instanceDirectory, boolean tcServer21orLater) {
		if (tcServer21orLater) {
			if (!new File(instanceDirectory, ".tc-runtime-instance").exists()) {
				return new Status(IStatus.ERROR, ITcServerConstants.PLUGIN_ID,
						"The specified server is not valid. The .tc-runtime-instance file is missing.");
			}
		}

		File confDir = new File(instanceDirectory, "conf");
		if (!confDir.exists()) {
			return new Status(IStatus.ERROR, ITcServerConstants.PLUGIN_ID,
					"The specified server is not valid. The conf directory is missing.");
		}
		File confFile = new File(confDir, "server.xml");
		if (!confFile.exists()) {
			return new Status(IStatus.ERROR, ITcServerConstants.PLUGIN_ID,
					"The specified server is not valid. The server.xml file in the conf directory is missing.");
		}
		if (!confFile.canRead()) {
			return new Status(IStatus.ERROR, ITcServerConstants.PLUGIN_ID,
					"The specified server is not valid. The server.xml file in the conf directory is not readable.");
		}
		return Status.OK_STATUS;
	}

	public static void executeInstanceCreation(IPath runtimeLocation, String instanceName, String[] arguments)
			throws CoreException {
		ServerInstanceCommand command = new ServerInstanceCommand(runtimeLocation.toFile());

		// execute
		int returnCode;
		try {
			returnCode = command.execute(arguments);
		}
		catch (Exception e) {
			throw handleResult(runtimeLocation, command, arguments, new Status(IStatus.ERROR,
					ITcServerConstants.PLUGIN_ID, "The instance creation command resulted in an exception", e));
		}

		if (returnCode != 0) {
			throw handleResult(runtimeLocation, command, arguments, new Status(IStatus.ERROR,
					ITcServerConstants.PLUGIN_ID, "The instance creation command failed and returned code "
							+ returnCode));
		}

		// verify result
		File instanceDirectory = getInstanceDirectory(arguments, instanceName);
		if (instanceDirectory == null) {
			instanceDirectory = new File(runtimeLocation.toFile(), instanceName);
		}
		IStatus status = validateInstance(instanceDirectory, true);
		if (!status.isOK()) {
			throw handleResult(runtimeLocation, command, arguments, status);
		}
	}

	private static File getInstanceDirectory(String[] arguments, String instanceName) {
		for (int i = 0; i < arguments.length; i++) {
			if (arguments[i].equals("-i") && arguments[i + 1] != null) {
				return new File(arguments[i + 1], instanceName);
			}
		}
		return null;
	}

	private static CoreException handleResult(IPath installLocation, ServerInstanceCommand command, String[] arguments,
			IStatus result) {
		StringBuilder cmdStr = new StringBuilder(command.toString());
		for (String arg : arguments) {
			cmdStr.append(' ');
			cmdStr.append(arg);
		}
		MultiStatus status = new MultiStatus(
				ITcServerConstants.PLUGIN_ID,
				0,
				NLS.bind(
						"Error creating server instance with command:\n \"{0}\". Check access permission for the directory {1} and its files and subdirectories.",
						new Object[] { cmdStr, installLocation }), null);
		if (result != null) {
			status.add(result);
		}
		IStatus output = new Status(IStatus.ERROR, ITcServerConstants.PLUGIN_ID,
				"Output of the instance creation command:\n" + command.getOutput());
		status.add(output);

		StatusHandler.log(status);

		return new CoreException(status);
	}

	public static String getTemplateName(File templateFolder) {
		if (templateFolder.isDirectory()) {
			int idx = templateFolder.getName().indexOf(TEMPLATE_VARIATION_STR);
			if (idx > -1) {
				return templateFolder.getName().substring(0, idx);
			}
			else {
				return templateFolder.getName();
			}
		}
		return null;
	}

	public static File getTemplateFolder(IRuntime runtime, String templateName) {
		StringBuilder templatePath = new StringBuilder();
		templatePath.append(runtime.getLocation());
		templatePath.append(File.separator);
		templatePath.append(TEMPLATES_FOLDER);
		templatePath.append(File.separator);
		templatePath.append(templateName);
		File templateFolder = new File(templatePath.toString());
		if (!templateFolder.exists() || !templateFolder.isDirectory()) {
			templateFolder = null;
			String serverVersion = getServerVersion(runtime);
			if (serverVersion != null && !serverVersion.isEmpty()) {
				templateFolder = null;
				int idx = serverVersion.indexOf('.');
				String majorVersion = idx > -1 ? serverVersion.substring(0, idx) : serverVersion;
				templatePath.append(TEMPLATE_VARIATION_STR);
				templatePath.append(majorVersion);
				templateFolder = new File(templatePath.toString());
				if (!templateFolder.exists() && !templateFolder.isDirectory()) {
					templateFolder = null;
				}
			}
		}
		return templateFolder;
	}

}
