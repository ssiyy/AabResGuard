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

import java.util.Collection;
import java.util.WeakHashMap;
import java.util.stream.Stream;

public abstract class WeakSet<T> implements Iterable<T> {



	protected WeakHashMap<T, T> weakHashStorage;

	public WeakSet() {
		weakHashStorage = new WeakHashMap<>();
	}

//==================================================================================================
// Interface Methods
//==================================================================================================	

	/**
	 * Add the given object to the set
	 * @param t the object to add
	 */
	public abstract void add(T t);

	/**
	 * Remove the given object from the data structure
	 * @param t the object to remove
	 * 
	 */
	public abstract void remove(T t);

	/**
	 * Returns true if the given object is in this data structure
	 * @return true if the given object is in this data structure
	 */
	public abstract boolean contains(T t);

	/**
	 * Remove all elements from this data structure
	 */
	public abstract void clear();

	/**
	 * Return the number of objects contained within this data structure
	 * @return the size
	 */
	public abstract int size();

	/**
	 * Return whether this data structure is empty
	 * @return whether this data structure is empty
	 */
	public abstract boolean isEmpty();

	/**
	 * Returns a Collection view of this set.  The returned Collection is backed by this set.
	 * 
	 * @return a Collection view of this set.  The returned Collection is backed by this set.
	 */
	public abstract Collection<T> values();

	/**
	 * Returns a stream of the values of this collection.
	 * @return a stream of the values of this collection.
	 */
	public Stream<T> stream() {
		return values().stream();
	}

	@Override
	public String toString() {
		return values().toString();
	}
}
