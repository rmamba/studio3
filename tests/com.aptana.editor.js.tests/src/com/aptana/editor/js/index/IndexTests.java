/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.index;

import junit.framework.Test;
import junit.framework.TestSuite;

public class IndexTests
{
	public static Test suite()
	{
		TestSuite suite = new TestSuite("Tests for com.aptana.editor.js.index");
		// $JUnit-BEGIN$
		suite.addTestSuite(JSCAIndexingTest.class);
		suite.addTestSuite(SDocMLIndexingTest.class);
		// $JUnit-END$
		return suite;
	}
}
