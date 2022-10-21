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

/**
 * <code>ArrayStringable</code> identifies those data types which when formed into
 * an array can be interpreted as a string (e.g., character array).  The {@link Array}
 * implementations will leverage this interface as both a marker and to generate appropriate
 * representations and values for data instances.
 */
public interface ArrayStringable extends DataType {

	/**
	 * For cases where an array of this type exists, determines if a String value
	 * will be returned.
	 * <p>
	 * @param settings
	 * @return true if array of this type with the specified settings will return
	 * a String value.
	 */
	public boolean hasStringValue(Settings settings);


	// ----------------------------------------------------------------------------
	//
	// Utility methods
	//
	// ----------------------------------------------------------------------------

	/**
	 * Get the ArrayStringable for a specified data type. Not used on an Array DataType, but
	 * on Array's element's type.
	 * <p>
	 * @param dt data type
	 * @return ArrayStringable object, or null.
	 */
	public static ArrayStringable getArrayStringable(DataType dt) {
		if (dt instanceof TypeDef) {
			dt = ((TypeDef) dt).getBaseDataType();
		}
		return (dt instanceof ArrayStringable) ? (ArrayStringable) dt : null;
	}

}
