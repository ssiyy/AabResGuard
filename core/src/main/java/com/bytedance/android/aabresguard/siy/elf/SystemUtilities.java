/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bytedance.android.aabresguard.siy.elf;

import java.awt.Font;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * General purpose class to provide convenience methods for doing "System" type
 * stuff, e.g., find resources, date/time, etc. All methods in this class are
 * static.
 */
public class SystemUtilities {

	private static String userName;

	/**
	 * System property that signals to override the font settings for Java and
	 * Ghidra components.
	 */
	public static final String FONT_SIZE_OVERRIDE_PROPERTY_NAME = "font.size.override";
	private static final Integer FONT_SIZE_OVERRIDE_VALUE =
		Integer.getInteger(SystemUtilities.FONT_SIZE_OVERRIDE_PROPERTY_NAME);

	/**
	 * The system property that can be checked during testing to determine if
	 * the system is running in test mode.
	 */
	public static final String TESTING_PROPERTY = "SystemUtilities.isTesting";

	/**
	 * The system property that can be checked during testing to determine if
	 * the system is running in batch, automated test mode.
	 */
	public static final String TESTING_BATCH_PROPERTY = "ghidra.test.property.batch.mode";

	/**
	 * The system property that can be checked during runtime to determine if we
	 * are running with a GUI or headless.
	 */
	public static final String HEADLESS_PROPERTY = "SystemUtilities.isHeadless";

	/**
	 * The system property that can be checked during runtime to determine if we
	 * are running in single-jar mode.
	 */
	public static final String SINGLE_JAR_MODE_PROPERTY = "SystemUtilities.isSingleJarMode";


	private static final boolean IS_IN_TESTING_BATCH_MODE =
		Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(TESTING_BATCH_PROPERTY));

	/**
	 * isInTestingMode - lazy load value - must allow time for runtime property to be set
	 * by GenericTestCase
	 */
	private static volatile Boolean isInTestingMode;


	/**
	 * Get the user that is running the ghidra application
	 * @return the user name
	 */
	public static String getUserName() {
		if (userName == null) {
			String uname = System.getProperty("user.name");

			// remove the spaces since some operating systems allow
			// spaces and some do not, Java's File class doesn't
			if (uname.indexOf(" ") >= 0) {
				userName = "";
				StringTokenizer tokens = new StringTokenizer(uname, " ", false);
				while (tokens.hasMoreTokens()) {
					userName += tokens.nextToken();
				}
			}
			else {
				userName = uname;
			}
		}
		return userName;
	}

	/**
	 * Gets the boolean value of the  system property by the given name.  If the property is
	 * not set, the defaultValue is returned.   If the value is set, then it will be passed
	 * into {@link Boolean#parseBoolean(String)}.
	 *
	 * @param name the property name to check
	 * @param defaultValue the default value
	 * @return true if the property is set and has a value of 'true', ignoring case
	 */
	public static boolean getBooleanProperty(String name, boolean defaultValue) {

		String value = System.getProperty(name);
		if (value == null) {
			return defaultValue;
		}

		return Boolean.parseBoolean(value);
	}

	/**
	 * Returns a non-null value if the system property is set that triggers the
	 * font override setting, which makes all Java and Ghidra component fonts
	 * the same size.
	 *
	 * @return a non-null value if the system property is set that triggers the
	 *         font override setting, which makes all Java and Ghidra component
	 *         fonts the same size.
	 * @see #FONT_SIZE_OVERRIDE_PROPERTY_NAME
	 */
	public static Integer getFontSizeOverrideValue() {
		return FONT_SIZE_OVERRIDE_VALUE;
	}

	/**
	 * Checks to see if the font size override setting is enabled and adjusts
	 * the given font as necessary to match the override setting. If the setting
	 * is not enabled, then <code>font</code> is returned.
	 *
	 * @param font
	 *            The current font to adjust, if necessary.
	 * @return a font object with the proper size.
	 */
	public static Font adjustForFontSizeOverride(Font font) {
		if (FONT_SIZE_OVERRIDE_VALUE == null) {
			return font;
		}

		return font.deriveFont((float) FONT_SIZE_OVERRIDE_VALUE.intValue());
	}

	/**
	 * Returns true if the system is running during a test.
	 *
	 * @return true if the system is running during a test.
	 */
	public static boolean isInTestingMode() {
		if (isInTestingMode == null) {
			isInTestingMode =
				Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(TESTING_PROPERTY));
		}
		return isInTestingMode;
	}

	/**
	 * Returns true if the system is running during a batch, automated test.
	 *
	 * @return true if the system is running during a batch, automated test.
	 */
	public static boolean isInTestingBatchMode() {
		return IS_IN_TESTING_BATCH_MODE;
	}

	/**
	 * Returns true if the system is running without a GUI.
	 *
	 * @return true if the system is running without a GUI.
	 */
	public static boolean isInHeadlessMode() {
		String headlessProperty = System.getProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
		return Boolean.parseBoolean(headlessProperty);
	}


	/**
	 * Returns whether or not the two indicated objects are equal. It allows
	 * either or both of the specified objects to be null.
	 *
	 * @param o1 the first object or null
	 * @param o2 the second object or null
	 * @return true if the objects are equal.
	 */
	public static boolean isEqual(Object o1, Object o2) {
		return Objects.equals(o1, o2);
	}

	public static <T extends Comparable<T>> int compareTo(T c1, T c2) {
		if (c1 == null) {
			return c2 == null ? 0 : 1;
		}
		else if (c2 == null) {
			return -1;
		}
		return c1.compareTo(c2);
	}

	public static boolean isArrayEqual(Object[] array1, Object[] array2) {
		if (array1 == null) {
			return (array2 == null);
		}
		if (array2 == null) {
			return false;
		}
		if (array1.length != array2.length) {
			return false;
		}
		for (int i = 0; i < array1.length; i++) {
			if (!isEqual(array1[i], array2[i])) {
				return false;
			}
		}
		return true;
	}


	/**
	 * Returns a file that contains the given class. If the class is in a jar file, then 
	 * the jar file will be returned. If the file is in a .class file, then the directory 
	 * containing the package root will be returned (i.e. the "bin" directory).
	 * 
	 * @param classObject the class for which to get the location
	 * @return the containing location
	 */
	public static File getSourceLocationForClass(Class<?> classObject) {
		String name = classObject.getName().replace('.', '/') + ".class";
		URL url = classObject.getClassLoader().getResource(name);

		String urlFile = url.getFile();
		try {
			urlFile = URLDecoder.decode(urlFile, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			// can't happen, since we know the encoding is correct
		}

		if ("file".equals(url.getProtocol())) {
			int packageLevel = getPackageLevel(classObject);
			File file = new File(urlFile);
			for (int i = 0; i < packageLevel + 1; i++) {
				file = file.getParentFile();
			}
			return file;
		}

		if ("jar".equals(url.getProtocol())) {
			// Running from Jar file
			String jarPath = urlFile;
			if (!jarPath.startsWith("file:")) {
				return null;
			}

			// strip off the 'file:' prefix and the jar path suffix after the
			// '!'
			jarPath = jarPath.substring(5, jarPath.indexOf('!'));
			return new File(jarPath);
		}

		return null;
	}

	private static int getPackageLevel(Class<?> classObject) {
		int dotCount = 0;
		Package package1 = classObject.getPackage();
		if (package1 == null) {
			return 0;
		}
		String packageName = package1.getName();
		for (int i = 0; i < packageName.length(); i++) {
			if (packageName.charAt(i) == '.') {
				dotCount++;
			}
		}
		return dotCount + 1;
	}


}
