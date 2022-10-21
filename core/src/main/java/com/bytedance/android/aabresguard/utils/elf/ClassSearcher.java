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
package com.bytedance.android.aabresguard.utils.elf;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.swing.event.ChangeListener;

/**
 * This class is a collection of static methods used to discover classes that implement a
 * particular interface or extend a particular base class.
 * <p>
 * <b>Warning: Using the search feature of this class will trigger other classes to be loaded.
 * Thus, clients should not make calls to this class inside of static initializer blocks</b>
 *
 * <p>Note: if your application is not using a module structure for its release build, then
 * your application must create the following file, with the required entries,
 * in order to find extension points:
 * <pre>
 * 	&lt;install dir&gt;/data/ExtensionPoint.manifest
 * </pre>
 *
 */
public class ClassSearcher {

	// This provides a means for custom apps that do not use a module structure to search all jars
	public static final String SEARCH_ALL_JARS_PROPERTY = "class.searcher.search.all.jars";
	private static final String SEARCH_ALL_JARS_PROPERTY_VALUE =
		System.getProperty(SEARCH_ALL_JARS_PROPERTY, Boolean.FALSE.toString());
	static final boolean SEARCH_ALL_JARS = Boolean.parseBoolean(SEARCH_ALL_JARS_PROPERTY_VALUE);




	private static List<Class<?>> extensionPoints;

	private static WeakSet<ChangeListener> listenerList =
		WeakDataStructureFactory.createCopyOnReadWeakSet();

	private static Pattern extensionPointSuffixPattern;

	private static volatile boolean hasSearched;
	private static volatile boolean isSearching;
	private static final ClassFilter DO_NOTHING_FILTER = c -> true;

	private ClassSearcher() {
		// you cannot create one of these
	}


	public static <T> List<Class<? extends T>> getClasses(Class<T> c) {
		return getClasses(c, null);
	}


	@SuppressWarnings("unchecked") // we checked the type of each use so we know the casts are safe
	public static <T> List<Class<? extends T>> getClasses(Class<T> c,
			Predicate<Class<? extends T>> classFilter) {
		if (isSearching) {
			throw new IllegalStateException(
				"Cannot call the getClasses() while the ClassSearcher is searching!");
		}

		List<Class<? extends T>> list = new ArrayList<>();
		if (extensionPoints == null) {
			return list;
		}

		for (Class<?> extensionPoint : extensionPoints) {
			if (c.isAssignableFrom(extensionPoint) &&
				(classFilter == null || classFilter.test((Class<T>) extensionPoint))) {
				list.add((Class<? extends T>) extensionPoint);
			}
		}
		return list;
	}

	public static <T> List<T> getInstances(Class<T> c) {
		return getInstances(c, DO_NOTHING_FILTER);
	}


	public static <T> List<T> getInstances(Class<T> c, ClassFilter filter) {
		List<Class<? extends T>> classes = getClasses(c);
		List<T> instances = new ArrayList<>();

		for (Class<? extends T> clazz : classes) {
			if (!filter.accepts(clazz)) {
				continue;
			}

			try {
				Constructor<? extends T> constructor = clazz.getConstructor((Class<?>[]) null);
				T t = constructor.newInstance((Object[]) null);
				instances.add(t);
			}
			catch (InstantiationException e) {

			}
			catch (IllegalAccessException e) {

			}
			catch (SecurityException e) {
				String message = "Error creating class " + clazz.getSimpleName() +
					" for extension " + c.getName() + ".  Security Exception!";


				throw new AssertException(message, e);
			}
			catch (Exception e) {

			}
		}

		return instances;

	}


}
