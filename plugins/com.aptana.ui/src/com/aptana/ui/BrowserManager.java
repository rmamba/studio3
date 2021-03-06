/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.internal.browser.BrowserDescriptorWorkingCopy;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.IBrowserDescriptorWorkingCopy;

import com.aptana.core.IMap;
import com.aptana.core.util.BrowserUtil;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.ExecutableUtil;
import com.aptana.core.util.IBrowserUtil.BrowserInfo;
import com.aptana.core.util.PlatformUtil;

/**
 * This is the BrowserManager counterpart to be used in Aptana Studio instead of the Eclipse BrowserManager. It wraps up
 * details on how we want to work with browsers (i.e.: smarter discovery and version numbers).
 * 
 * @author Fabio
 */
@SuppressWarnings("restriction")
public class BrowserManager implements IBrowserProvider
{

	private static IBrowserProvider instance;
	private static final String SAFARI_APP = "Safari.app"; //$NON-NLS-1$
	private static final String SAFARI_PARAMS = "-a safari "; //$NON-NLS-1$
	private static String openCommandPath;

	public static synchronized IBrowserProvider getInstance()
	{
		if (instance == null)
		{
			instance = new BrowserManager();
		}
		return instance;
	}

	private BrowserManager()
	{
		IPath commandPath = ExecutableUtil.find("open", false, null); //$NON-NLS-1$
		if (commandPath != null)
		{
			openCommandPath = commandPath.toOSString();
		}
	}

	private String getRealPath(String loc)
	{
		if (loc == null)
		{
			return null; // default browser in eclipse
		}
		File file = new File(loc);
		String path;
		try
		{
			// Resolve links to see actual location
			path = file.getCanonicalPath();
			if (PlatformUtil.isMac() && path.contains(SAFARI_APP))
			{
				path = openCommandPath;
			}
		}
		catch (IOException e)
		{
			path = file.getAbsolutePath();
		}
		return path;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ui.IBrowserProvider#searchMoreBrowsers()
	 */
	public Collection<BrowserInfo> searchMoreBrowsers()
	{
		org.eclipse.ui.internal.browser.BrowserManager eclipseBrowserManager = org.eclipse.ui.internal.browser.BrowserManager
				.getInstance();
		List<IBrowserDescriptor> webBrowsers = eclipseBrowserManager.getWebBrowsers();
		Set<String> currentBrowsers = new HashSet<String>();
		for (IBrowserDescriptor iBrowserDescriptor : webBrowsers)
		{
			String path = getRealPath(iBrowserDescriptor.getLocation());
			if (path == null)
			{
				continue;
			}
			currentBrowsers.add(path);
		}

		Collection<BrowserInfo> browsersFound = new ArrayList<BrowserInfo>();

		List<BrowserInfo> discoverInstalledBrowsers = BrowserUtil.discoverInstalledBrowsers();
		boolean needSave = false;
		for (BrowserInfo browserInfo : discoverInstalledBrowsers)
		{
			String browserLocation = getRealPath(browserInfo.getLocation());
			if (browserLocation != null && !currentBrowsers.contains(browserLocation))
			{
				currentBrowsers.add(browserLocation);
				BrowserDescriptorWorkingCopy workingCopy = new BrowserDescriptorWorkingCopy();
				workingCopy.setName(browserInfo.getName());
				workingCopy.setLocation(browserInfo.getLocation());
				workingCopy.save();
				browsersFound.add(browserInfo);
				needSave = true;
			}
		}
		if (needSave)
		{
			// forces a save on the new list of browsers
			eclipseBrowserManager.setCurrentWebBrowser(eclipseBrowserManager.getCurrentWebBrowser());
		}
		verifyBrowserConfigurations();
		return browsersFound;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ui.IBrowserProvider#getWebBrowsers()
	 */
	public List<BrowserInfo> getWebBrowsers()
	{
		List<IBrowserDescriptor> webBrowsers = org.eclipse.ui.internal.browser.BrowserManager.getInstance()
				.getWebBrowsers();
		return CollectionsUtil.map(webBrowsers, new IMap<IBrowserDescriptor, BrowserInfo>()
		{
			public BrowserInfo map(IBrowserDescriptor browser)
			{
				return new BrowserInfo(browser.getName(), browser.getLocation());
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.ui.IBrowserProvider#getCurrentWebBrowser()
	 */
	public BrowserInfo getCurrentWebBrowser()
	{
		IBrowserDescriptor currentWebBrowser = org.eclipse.ui.internal.browser.BrowserManager.getInstance()
				.getCurrentWebBrowser();
		return new BrowserInfo(currentWebBrowser.getName(), currentWebBrowser.getLocation());
	}

	public String getBrowserVersion(BrowserInfo info)
	{
		return BrowserUtil.getBrowserVersion(info);
	}

	@SuppressWarnings("restriction")
	public void verifyBrowserConfigurations()
	{
		if (PlatformUtil.isMac())
		{
			List<IBrowserDescriptor> browsers = org.eclipse.ui.internal.browser.BrowserManager.getInstance()
					.getWebBrowsers();
			for (IBrowserDescriptor browser : browsers)
			{
				if (browser.getLocation() != null && browser.getLocation().contains(SAFARI_APP))
				{
					IBrowserDescriptorWorkingCopy safariWorkingcopy = browser.getWorkingCopy();
					safariWorkingcopy.setLocation(openCommandPath);
					safariWorkingcopy.setParameters(SAFARI_PARAMS);
					safariWorkingcopy.save();
				}
			}
		}
	}
}
