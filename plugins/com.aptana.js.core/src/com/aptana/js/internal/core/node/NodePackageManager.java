/**
 * Aptana Studio
 * Copyright (c) 2012-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.internal.core.node;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.aptana.core.IMap;
import com.aptana.core.ShellExecutable;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.ExecutableUtil;
import com.aptana.core.util.PlatformUtil;
import com.aptana.core.util.ProcessRunnable;
import com.aptana.core.util.ProcessStatus;
import com.aptana.core.util.ProcessUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.core.util.SudoCommandProcessRunnable;
import com.aptana.core.util.VersionUtil;
import com.aptana.js.core.JSCorePlugin;
import com.aptana.js.core.node.INodePackageManager;

/**
 * @author cwilliams
 */
public class NodePackageManager implements INodePackageManager
{

	/**
	 * Config value holding location where we install bianries/modules.
	 */
	private static final String PREFIX = "prefix"; //$NON-NLS-1$

	/**
	 * ENV variable that can override prefix config value.
	 */
	private static final String NPM_CONFIG_PREFIX = "NPM_CONFIG_PREFIX"; //$NON-NLS-1$

	/**
	 * Folder where modules live.
	 */
	private static final String NODE_MODULES = "node_modules"; //$NON-NLS-1$

	private static final String BIN = "bin"; //$NON-NLS-1$
	private static final String LIB = "lib"; //$NON-NLS-1$

	/**
	 * Argument to {@code COLOR} switch/config option so that ANSI colors aren't used in output.
	 */
	private static final String FALSE = "false"; //$NON-NLS-1$

	/**
	 * Special switch/config option to set ANSI color option. Set to {@code FALSE} to disable ANSI color output.
	 */
	private static final String COLOR = "--color"; //$NON-NLS-1$

	private static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)"); //$NON-NLS-1$

	/**
	 * Common install location for Windows
	 */
	private static final String APPDATA_NPM = "%APPDATA%\\npm"; //$NON-NLS-1$

	/**
	 * Coommon install location for Mac/Linux
	 */
	private static final String USR_LOCAL_BIN_NPM = "/usr/local/bin/npm"; //$NON-NLS-1$

	/**
	 * Binary script name.
	 */
	private static final String NPM = "npm"; //$NON-NLS-1$

	/**
	 * Commands
	 */
	private static final String INSTALL = "install"; //$NON-NLS-1$
	private static final String LIST = "list"; //$NON-NLS-1$

	private IPath npmPath;

	/**
	 * Cached value for NPM's "prefix" config value (where modules get installed).
	 */
	private IPath fConfigPrefixPath;

	/*
	 * (non-Javadoc)
	 * @see com.appcelerator.titanium.nodejs.core.INodePackageManager#install(com.appcelerator.titanium.nodejs.core.
	 * NPMInstallerCommand, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus install(String packageName, String displayName, boolean global, char[] password,
			IProgressMonitor monitor)
	{
		return install(packageName, displayName, global, password, null, monitor);
	}

	public IStatus install(String packageName, String displayName, boolean global, char[] password,
			IPath workingDirectory, IProgressMonitor monitor)
	{
		SubMonitor sub = SubMonitor.convert(monitor,
				MessageFormat.format(Messages.NodePackageManager_InstallingTaskName, displayName), 100);
		try
		{
			IPath npmPath = findNPM();
			if (npmPath == null)
			{
				throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
						Messages.NodePackageManager_ERR_NPMNotInstalled));
			}

			List<String> args = new ArrayList<String>(8);
			if (global)
			{
				if (PlatformUtil.isMac() || PlatformUtil.isLinux())
				{
					args.add("sudo"); //$NON-NLS-1$
					args.add("-S"); //$NON-NLS-1$
					args.add("--"); //$NON-NLS-1$
				}
				args.add(npmPath.toOSString());
				args.add(GLOBAL_ARG);
			}
			else
			{
				args.add(npmPath.toOSString());
			}
			CollectionsUtil.addToList(args, INSTALL, packageName, COLOR, FALSE);
			args.addAll(proxySettings());

			Map<String, String> environment = ShellExecutable.getEnvironment();
			environment.put(ProcessUtil.REDIRECT_ERROR_STREAM, StringUtil.EMPTY);

			// HACK for TISTUD-4101
			if (PlatformUtil.isWindows())
			{
				IPath pythonExe = ExecutableUtil.find("pythonw.exe", false, null); //$NON-NLS-1$
				if (pythonExe == null)
				{
					// Add python to PATH
					Bundle bundle = Platform.getBundle("com.appcelerator.titanium.python.win32"); //$NON-NLS-1$
					if (bundle != null)
					{
						// Windows is wonderful, it sometimes stores in "Path" and "PATH" doesn't work
						String pathName = "PATH"; //$NON-NLS-1$
						if (!environment.containsKey(pathName))
						{
							pathName = "Path"; //$NON-NLS-1$
						}
						String path = environment.get(pathName);

						IPath relative = new Path("."); //$NON-NLS-1$
						URL bundleURL = FileLocator.find(bundle, relative, null);
						URL fileURL = FileLocator.toFileURL(bundleURL);
						File f = new File(fileURL.getPath());
						if (f.exists())
						{
							path = path + File.pathSeparator + new File(f, "python").getCanonicalPath(); //$NON-NLS-1$
							environment.put(pathName, path);
						}
					}
				}
			}

			Process p = ProcessUtil.run(args.get(0), workingDirectory, environment, args.subList(1, args.size())
					.toArray(new String[args.size() - 1]));
			sub.worked(5);

			ProcessRunnable runnable;
			if (global)
			{
				runnable = new SudoCommandProcessRunnable(p, sub.newChild(95), true, password);
			}
			else
			{
				runnable = new ProcessRunnable(p, sub.newChild(95), true);
			}
			Thread t = new Thread(runnable, MessageFormat.format("{0} installer", displayName)); //$NON-NLS-1$
			t.start();
			t.join();

			IStatus status = runnable.getResult();
			if (status.getSeverity() == IStatus.CANCEL)
			{
				return Status.OK_STATUS;
			}
			if (!status.isOK())
			{
				String message;
				if (status instanceof ProcessStatus)
				{
					message = ((ProcessStatus) status).getStdErr();
				}
				else
				{
					message = status.getMessage();
				}
				IdeLog.logError(JSCorePlugin.getDefault(),
						MessageFormat.format("Failed to install {0}.\n\n{1}", packageName, message)); //$NON-NLS-1$
				return new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, MessageFormat.format(
						Messages.NodePackageManager_FailedInstallError, packageName));
			}
			else if (status instanceof ProcessStatus)
			{
				String error = ((ProcessStatus) status).getStdErr();
				if (!StringUtil.isEmpty(error))
				{
					String[] lines = error.split("\n"); //$NON-NLS-1$
					if (lines.length > 0 && lines[lines.length - 1].contains("ERR!")) //$NON-NLS-1$
					{
						IdeLog.logError(JSCorePlugin.getDefault(),
								MessageFormat.format("Failed to install {0}.\n\n{1}", packageName, error)); //$NON-NLS-1$
						return new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, MessageFormat.format(
								Messages.NodePackageManager_FailedInstallError, packageName));
					}
				}
			}
			return status;
		}
		catch (CoreException ce)
		{
			return ce.getStatus();
		}
		catch (Exception e)
		{
			return new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, e.getMessage(), e);
		}
		finally
		{
			sub.done();
		}
	}

	/**
	 * This will return a list of arguments for proxy settings (if we have any, otherwise an empty list).
	 */
	private List<String> proxySettings()
	{
		IProxyService service = JSCorePlugin.getDefault().getProxyService();
		if (service == null || !service.isProxiesEnabled())
		{
			return Collections.emptyList();
		}

		List<String> proxyArgs = new ArrayList<String>(4);
		IProxyData httpData = service.getProxyData(IProxyData.HTTP_PROXY_TYPE);
		if (httpData != null && httpData.getHost() != null)
		{
			CollectionsUtil.addToList(proxyArgs, "--proxy", buildProxyURL(httpData)); //$NON-NLS-1$
		}
		IProxyData httpsData = service.getProxyData(IProxyData.HTTPS_PROXY_TYPE);
		if (httpsData != null && httpsData.getHost() != null)
		{
			CollectionsUtil.addToList(proxyArgs, "--https-proxy", buildProxyURL(httpsData)); //$NON-NLS-1$
		}
		return proxyArgs;
	}

	/**
	 * Given proxy data, we try to convert that back into a full URL
	 * 
	 * @param data
	 * @return
	 */
	private String buildProxyURL(IProxyData data)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("http://"); //$NON-NLS-1$
		if (!StringUtil.isEmpty(data.getUserId()))
		{
			builder.append(data.getUserId());
			builder.append(':');
			builder.append(data.getPassword());
			builder.append('@');
		}
		builder.append(data.getHost());
		if (data.getPort() != -1)
		{
			builder.append(':');
			builder.append(data.getPort());
		}
		return builder.toString();
	}

	public IPath findNPM()
	{
		if (npmPath == null)
		{
			List<IPath> commonLocations;
			if (PlatformUtil.isWindows())
			{
				commonLocations = CollectionsUtil.newList(Path.fromOSString(PlatformUtil
						.expandEnvironmentStrings(APPDATA_NPM)));
				IPath nodePath = JSCorePlugin.getDefault().getNodeJSService().getValidExecutable();
				if (nodePath != null)
				{
					nodePath = nodePath.removeLastSegments(1);// Remove file extension.
					commonLocations.add(nodePath);
					ShellExecutable.updatePathEnvironment(PlatformUtil.expandEnvironmentStrings(APPDATA_NPM));
				}
			}
			else
			{
				commonLocations = CollectionsUtil.newList(Path.fromOSString(USR_LOCAL_BIN_NPM));
			}
			IPath path = ExecutableUtil.find(NPM, true, commonLocations);
			if (path != null && path.toFile().exists())
			{
				npmPath = path;
			}
		}
		return npmPath;
	}

	public Set<String> list(boolean global) throws CoreException
	{
		IPath npmPath = findNPM();
		if (npmPath == null)
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
					Messages.NodePackageManager_ERR_NPMNotInstalled));
		}
		List<String> args = CollectionsUtil.newList(PARSEABLE_ARG, LIST);
		if (global)
		{
			args.add(0, GLOBAL_ARG);
		}

		IStatus status = ProcessUtil.runInBackground(npmPath.toOSString(), null, ShellExecutable.getEnvironment(),
				args.toArray(new String[args.size()]));
		if (!status.isOK())
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
					Messages.NodePackageManager_FailedListingError));
		}

		// Need to parse the output!
		String output = status.getMessage();
		String[] lines = StringUtil.LINE_SPLITTER.split(output);
		List<IPath> paths = CollectionsUtil.map(CollectionsUtil.newSet(lines), new IMap<String, IPath>()
		{
			public IPath map(String item)
			{
				return Path.fromOSString(item);
			}
		});
		Set<String> installed = new HashSet<String>(paths.size());
		for (IPath path : paths)
		{
			try
			{
				// The paths we get are locations on disk. We can tell a module's name by looking for a path
				// that is a child of 'nod_modules', i.e. "/usr/local/lib/node_modules/alloy"
				if (NODE_MODULES.equals(path.segment(path.segmentCount() - 2)))
				{
					installed.add(path.lastSegment());
				}
			}
			catch (Exception e)
			{
				// There is a chance that npm throw warnings if there are any partial installations
				// and npm might fail while trying to parse those warnings.
				if (!path.toOSString().contains("npm WARN")) //$NON-NLS-1$
				{
					throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, e.getMessage()));
				}
			}
		}
		return installed;
	}

	public boolean isInstalled(String packageName) throws CoreException
	{
		try
		{
			Version version = getInstalledVersion(packageName);
			if (version != null)
			{
				return true;
			}
		}
		catch (CoreException e)
		{
			IdeLog.logInfo(JSCorePlugin.getDefault(), MessageFormat.format(
					"Error getting the installed version of package {0}; falling back to use ''npm list''", //$NON-NLS-1$
					packageName));
		}
		Set<String> listing = list(true);
		return listing.contains(packageName);
	}

	public IPath getModulesPath(String packageName) throws CoreException
	{
		IPath npmPath = findNPM();
		if (npmPath == null)
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
					Messages.NodePackageManager_ERR_NPMNotInstalled));
		}
		IStatus status = ProcessUtil.runInBackground(npmPath.toOSString(), null, ShellExecutable.getEnvironment(),
				PARSEABLE_ARG, LIST, packageName, GLOBAL_ARG);
		if (!status.isOK())
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, MessageFormat.format(
					Messages.NodePackageManager_FailedListPackageError, packageName)));
		}
		String message = status.getMessage();
		String[] lines = message.split("\n"); //$NON-NLS-1$
		return Path.fromOSString(lines[lines.length - 1]);
	}

	public Version getInstalledVersion(String packageName) throws CoreException
	{
		IPath npmPath = findNPM();
		if (npmPath == null)
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
					Messages.NodePackageManager_ERR_NPMNotInstalled));
		}
		IStatus status = ProcessUtil.runInBackground(npmPath.toOSString(), null, ShellExecutable.getEnvironment(),
				"ls", GLOBAL_ARG, packageName, COLOR, FALSE); //$NON-NLS-1$
		if (!status.isOK())
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, MessageFormat.format(
					Messages.NodePackageManager_FailedToDetermineInstalledVersion, packageName)));
		}
		String output = status.getMessage();
		int index = output.indexOf(packageName + '@');
		if (index != -1)
		{
			output = output.substring(index + packageName.length() + 1);
			int space = output.indexOf(' ');
			if (space != -1)
			{
				output = output.substring(0, space);
			}
			return VersionUtil.parseVersion(output);
		}
		return null;
	}

	public Version getLatestVersionAvailable(String packageName) throws CoreException
	{
		// get the latest version
		// npm view titanium version
		IPath npmPath = findNPM();
		if (npmPath == null)
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
					Messages.NodePackageManager_ERR_NPMNotInstalled));
		}
		List<String> args = CollectionsUtil.newList("view", packageName, "version");//$NON-NLS-1$ //$NON-NLS-2$
		args.addAll(proxySettings());

		IStatus status = ProcessUtil.runInBackground(npmPath.toOSString(), null, ShellExecutable.getEnvironment(),
				args.toArray(new String[args.size()]));
		if (!status.isOK())
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, MessageFormat.format(
					Messages.NodePackageManager_FailedToDetermineLatestVersion, packageName)));
		}
		String message = status.getMessage().trim();
		Matcher m = VERSION_PATTERN.matcher(message);
		if (m.find())
		{
			return VersionUtil.parseVersion(m.group(1));
		}
		return null;
	}

	public String getConfigValue(String key) throws CoreException
	{
		// npm config get <key>
		IPath npmPath = findNPM();
		if (npmPath == null)
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID,
					Messages.NodePackageManager_ERR_NPMNotInstalled));
		}
		IStatus status = ProcessUtil.runInBackground(npmPath.toOSString(), null, ShellExecutable.getEnvironment(),
				"config", "get", key); //$NON-NLS-1$ //$NON-NLS-2$
		if (!status.isOK())
		{
			throw new CoreException(new Status(IStatus.ERROR, JSCorePlugin.PLUGIN_ID, MessageFormat.format(
					"Failed to get value of npm config key {0}", key)));
		}
		return status.getMessage().trim();
	}

	// When in global mode, executables are linked into {prefix}/bin on Unix, or directly into {prefix} on Windows.
	public IPath getBinariesPath() throws CoreException
	{
		IPath prefix = getConfigPrefixPath();
		if (prefix == null)
		{
			return null;
		}

		if (PlatformUtil.isWindows())
		{
			return prefix;
		}
		return prefix.append(BIN);
	}

	// Global installs on Unix systems go to {prefix}/lib/node_modules. Global installs on Windows go to
	// {prefix}/node_modules (that is, no lib folder.)
	public IPath getModulesPath() throws CoreException
	{
		IPath prefix = getConfigPrefixPath();
		if (prefix == null)
		{
			return null;
		}

		if (PlatformUtil.isWindows())
		{
			return prefix.append(NODE_MODULES);
		}
		return prefix.append(LIB).append(NODE_MODULES);
	}

	public synchronized IPath getConfigPrefixPath() throws CoreException
	{
		if (fConfigPrefixPath == null)
		{
			String npmConfigPrefixPath = ShellExecutable.getEnvironment().get(NPM_CONFIG_PREFIX);
			if (npmConfigPrefixPath != null)
			{
				fConfigPrefixPath = Path.fromOSString(npmConfigPrefixPath);
			}
			else
			{
				fConfigPrefixPath = Path.fromOSString(getConfigValue(PREFIX));
			}
		}
		return fConfigPrefixPath;
	}
}
