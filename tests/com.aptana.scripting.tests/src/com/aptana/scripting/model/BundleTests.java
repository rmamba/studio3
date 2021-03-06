/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.scripting.model;

import java.util.LinkedList;
import java.util.List;

import com.aptana.scripting.ScriptLogListener;
import com.aptana.scripting.model.filters.IModelFilter;

public class BundleTests extends BundleTestBase
{
	public class LogListener implements ScriptLogListener
	{
		List<String> errors = new LinkedList<String>();
		List<String> infos = new LinkedList<String>();
		List<String> warnings = new LinkedList<String>();
		List<String> prints = new LinkedList<String>();
		List<String> printErrors = new LinkedList<String>();
		List<String> traces = new LinkedList<String>();

		public void logError(String error)
		{
			this.errors.add(error);
		}

		public void logInfo(String info)
		{
			this.infos.add(info);
		}

		public void logWarning(String warning)
		{
			this.warnings.add(warning);
		}

		public void print(String message)
		{
			this.prints.add(message);
		}

		public void printError(String message)
		{
			this.printErrors.add(message);
		}

		public void trace(String message)
		{
			this.traces.add(message);
		}

		public void reset()
		{
			this.errors.clear();
			this.infos.clear();
			this.warnings.clear();
			this.prints.clear();
			this.printErrors.clear();
			this.traces.clear();
		}
	};

	/**
	 * compareScopedBundles
	 * 
	 * @param bundleName
	 * @param prec1
	 * @param prec2
	 * @param command1
	 * @param command2
	 */
	private void compareScopedBundles(String bundleName, BundlePrecedence prec1, BundlePrecedence prec2,
			String command1, String command2)
	{
		// confirm first bundle loaded properly
		BundleEntry entry = this.getBundleEntry(bundleName, prec1);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals(command1, commands.get(0).getInvoke());

		// confirm second bundle overrides application
		entry = this.getBundleEntry(bundleName, prec2);
		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals(command2, commands.get(0).getInvoke());
	}

	/**
	 * compareScopedBundlesWithDelete
	 * 
	 * @param bundleName
	 * @param prec1
	 * @param prec2
	 * @param command1
	 * @param command2
	 */
	private void compareScopedBundlesWithDelete(String bundleName, BundlePrecedence prec1, BundlePrecedence prec2,
			String command1, String command2)
	{
		this.compareScopedBundles(bundleName, prec1, prec2, command1, command2);

		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry(bundleName);
		List<BundleElement> bundles = entry.getBundles();
		assertEquals(2, bundles.size());
		entry.removeBundle(bundles.get(1));

		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals(command1, commands.get(0).getInvoke());
	}

	/**
	 * setUp
	 */
	protected void setUp() throws Exception
	{
		super.setUp();

		BundleTestBase.getBundleManagerInstance().reset();
	}

	/**
	 * testLoadLoneBundle
	 */
	public void testLoadLoneBundle()
	{
		String bundleName = "loneBundle";
		BundleElement bundle = this.loadBundle(bundleName, BundlePrecedence.APPLICATION);

		assertNotNull(bundle);
		assertEquals(bundleName, bundle.getDisplayName());
	}

	/**
	 * testLoadBundleWithCommand
	 */
	public void testLoadBundleWithCommand()
	{
		String bundleName = "bundleWithCommand";
		BundleEntry entry = this.getBundleEntry(bundleName, BundlePrecedence.APPLICATION);
		List<CommandElement> commands = entry.getCommands();

		assertNotNull(commands);
		assertEquals(1, commands.size());
	}

	/**
	 * testLoadBundleWithMenu
	 */
	public void testLoadBundleWithMenu()
	{
		String bundleName = "bundleWithMenu";
		BundleEntry entry = this.getBundleEntry(bundleName, BundlePrecedence.APPLICATION);
		List<MenuElement> menus = entry.getMenus();

		assertNotNull(menus);
		assertEquals(1, menus.size());
	}

	/**
	 * testLoadBundleWithSnippet
	 */
	public void testLoadBundleWithSnippet()
	{
		String bundleName = "bundleWithSnippet";
		BundleEntry entry = this.getBundleEntry(bundleName, BundlePrecedence.APPLICATION);
		List<CommandElement> snippets = entry.getCommands();

		assertNotNull(snippets);
		assertEquals(1, snippets.size());
		assertTrue(snippets.get(0) instanceof SnippetElement);

		List<SnippetElement> snippetElements = entry.getSnippets();
		assertNotNull(snippetElements);
		assertEquals(1, snippetElements.size());
	}

	/**
	 * testLoadBundleWithSnippetUsingFilter
	 */
	public void testLoadBundleWithSnippetUsingFilter()
	{
		String bundleName = "bundleWithSnippet";
		BundleEntry entry = this.getBundleEntry(bundleName, BundlePrecedence.APPLICATION);
		List<SnippetElement> snippets = BundleManager.getInstance().getSnippets(new IModelFilter()
		{

			public boolean include(AbstractElement element)
			{
				return element instanceof SnippetElement;
			}
		});

		assertNotNull(snippets);
		assertEquals(1, snippets.size());
	}

	/**
	 * testLoadBundleWithSnippetUsingFilter
	 */
	public void testLoadBundleWithSnippetUsingNullFilter()
	{
		String bundleName = "bundleWithSnippet";
		BundleEntry entry = this.getBundleEntry(bundleName, BundlePrecedence.APPLICATION);
		List<SnippetElement> snippets = BundleManager.getInstance().getSnippets(null);

		assertNotNull(snippets);
		assertEquals(1, snippets.size());
	}

	/**
	 * testUserOverridesApplication
	 */
	public void testUserOverridesApplication()
	{
		compareScopedBundles("bundleWithCommand", BundlePrecedence.APPLICATION, BundlePrecedence.USER, "cd", "cd ..");
	}

	/**
	 * testUserOverridesApplication2
	 */
	public void testUserOverridesApplication2()
	{
		this.compareScopedBundles("bundleWithCommand", BundlePrecedence.USER, BundlePrecedence.APPLICATION, "cd ..",
				"cd ..");
	}

	/**
	 * testUserOverridesApplication
	 */
	public void testProjectOverridesApplication()
	{
		this.compareScopedBundles("bundleWithCommand", BundlePrecedence.APPLICATION, BundlePrecedence.PROJECT, "cd",
				"cd /");
	}

	/**
	 * testUserOverridesApplication2
	 */
	public void testProjectOverridesApplication2()
	{
		this.compareScopedBundles("bundleWithCommand", BundlePrecedence.PROJECT, BundlePrecedence.APPLICATION, "cd /",
				"cd /");
	}

	/**
	 * testUserOverridesApplication
	 */
	public void testProjectOverridesUser()
	{
		this.compareScopedBundles("bundleWithCommand", BundlePrecedence.USER, BundlePrecedence.PROJECT, "cd ..", "cd /");
	}

	/**
	 * testUserOverridesApplication2
	 */
	public void testProjectOverridesUser2()
	{
		this.compareScopedBundles("bundleWithCommand", BundlePrecedence.PROJECT, BundlePrecedence.USER, "cd /", "cd /");
	}

	/**
	 * testApplicationOverrideAndDelete
	 */
	public void testApplicationOverrideAndDelete()
	{
		this.compareScopedBundlesWithDelete("bundleWithCommand", BundlePrecedence.APPLICATION, BundlePrecedence.USER,
				"cd", "cd ..");
	}

	/**
	 * testApplicationOverrideAndDelete
	 */
	public void testApplicationOverrideAndDelete2()
	{
		this.compareScopedBundlesWithDelete("bundleWithCommand", BundlePrecedence.APPLICATION,
				BundlePrecedence.PROJECT, "cd", "cd /");
	}

	/**
	 * testUserOverridesApplication
	 */
	public void testUserOverrideAndDelete()
	{
		this.compareScopedBundlesWithDelete("bundleWithCommand", BundlePrecedence.USER, BundlePrecedence.PROJECT,
				"cd ..", "cd /");
	}

	/**
	 * testSamePrecedenceOverride
	 */
	public void testSamePrecedenceOverride()
	{
		// confirm first bundle loaded properly
		BundleEntry entry = this.getBundleEntry("bundleWithCommand", BundlePrecedence.USER);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals("cd ..", commands.get(0).getInvoke());

		// confirm second bundle overrides application
		this.loadBundleEntry("bundleWithSameCommand", BundlePrecedence.USER);
		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals("cd", commands.get(0).getInvoke());
	}

	/**
	 * testSamePrecedenceOverride2
	 */
	public void testSamePrecedenceOverride2()
	{
		// confirm first bundle loaded properly
		this.loadBundleEntry("bundleWithSameCommand", BundlePrecedence.USER);
		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry("bundleWithCommand");
		assertNotNull(entry);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals("cd", commands.get(0).getInvoke());

		// confirm second bundle overrides application
		this.loadBundleEntry("bundleWithCommand", BundlePrecedence.USER);
		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals("cd", commands.get(0).getInvoke());
	}

	/**
	 * testSamePrecedenceAugmentation
	 */
	public void testSamePrecedenceAugmentation()
	{
		// confirm first bundle loaded properly
		BundleEntry entry = this.getBundleEntry("bundleWithCommand", BundlePrecedence.USER);
		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(1, commands.size());
		assertEquals("cd ..", commands.get(0).getInvoke());

		// confirm second bundle overrides application
		this.loadBundleEntry("bundleWithDifferentCommand", BundlePrecedence.USER);
		commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(2, commands.size());

		CommandElement command1 = commands.get(0);
		CommandElement command2 = commands.get(1);

		if (command1.getDisplayName().equals("MyCommand"))
		{
			assertEquals("cd ..", command1.getInvoke());
			assertEquals("cd", command2.getInvoke());
		}
		else
		{
			assertEquals("cd", command1.getInvoke());
			assertEquals("cd ..", command2.getInvoke());
		}
	}

	/**
	 * testBundleInCommandsDirectory
	 */
	// FIXME not working ATM
	// public void testBundleInCommandsDirectory()
	// {
	// LogListener listener = new LogListener();
	// ScriptLogger.getInstance().addLogListener(listener);
	// this.loadBundleEntry("bundleInCommands", BundlePrecedence.PROJECT);
	//
	// assertEquals(1, listener.errors.size());
	// assertTrue(listener.errors.get(0).contains(
	// "Attempted to define a bundle in a file other than the bundle's bundle.rb file:"));
	// }

	/**
	 * testBundleFileInCommandsDirectory
	 */
	public void testBundleFileInCommandsDirectory()
	{
		// LogListener listener = new LogListener();
		// ScriptLogger.getInstance().addLogListener(listener);
		// this.loadBundleEntry("bundleFileInCommands", BundlePrecedence.PROJECT);

		// assertEquals(1, listener.errors.size());
		// assertTrue(listener.errors.get(0).contains(
		// "Attempted to define a bundle in a file other than the bundle's bundle.rb file:"));
	}

	/**
	 * testNameFromBundleDirectory
	 */
	public void testNameFromBundleDirectory()
	{
		// load bundle
		String bundleName = "bundleName";
		this.loadBundleEntry(bundleName, BundlePrecedence.PROJECT);

		// get bundle entry
		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry(bundleName);
		assertNotNull(entry);
	}

	/**
	 * testNameFromBundleDirectoryWithExtension
	 */
	public void testNameFromBundleDirectoryWithExtension()
	{
		// load bundle
		this.loadBundleEntry("bundleNameWithExtension.ruble", BundlePrecedence.PROJECT);

		// get bundle entry
		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry("bundleNameWithExtension");
		assertNotNull(entry);
	}

	/**
	 * testBundleIsBundleDeclaration
	 */
	public void testBundleIsBundleDeclaration()
	{
		String bundleName = "bundleDefinition";
		this.loadBundleEntry(bundleName, BundlePrecedence.PROJECT);

		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry(bundleName);
		assertNotNull(entry);

		List<BundleElement> bundles = entry.getBundles();
		assertNotNull(bundles);
		assertEquals(1, bundles.size());
		assertFalse(bundles.get(0).isReference());
	}

	/**
	 * testBundleIsBundleDeclaration2
	 */
	public void testBundleIsBundleDeclaration2()
	{
		String bundleName = "bundleDefinition2";
		this.loadBundleEntry(bundleName, BundlePrecedence.PROJECT);

		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry(bundleName);
		assertNotNull(entry);

		List<BundleElement> bundles = entry.getBundles();
		assertNotNull(bundles);
		assertEquals(1, bundles.size());
		assertFalse(bundles.get(0).isReference());
	}

	/**
	 * testBundleIsBundleReference
	 */
	public void testBundleIsBundleReference()
	{
		this.loadBundleEntry("bundleReference", BundlePrecedence.PROJECT);

		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry("MyBundle");
		assertNotNull(entry);

		List<BundleElement> bundles = entry.getBundles();
		assertNotNull(bundles);
		assertEquals(1, bundles.size());
		assertTrue(bundles.get(0).isReference());
	}

	public void testReferenceLoadingAcrossPrecendenceBounds()
	{
		this.loadBundleEntry("bundleWithCommand", BundlePrecedence.APPLICATION);
		this.loadBundleEntry("bundleWithCommandReference", BundlePrecedence.APPLICATION);
		this.loadBundleEntry("bundleWithCommandReference", BundlePrecedence.PROJECT);

		BundleEntry entry = BundleTestBase.getBundleManagerInstance().getBundleEntry("bundleWithCommand");
		assertNotNull(entry);

		List<BundleElement> bundles = entry.getBundles();
		assertNotNull(bundles);
		assertEquals(3, bundles.size());

		List<CommandElement> commands = entry.getCommands();
		assertNotNull(commands);
		assertEquals(3, commands.size());
	}
}
