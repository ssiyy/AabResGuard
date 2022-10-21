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
 * Array interface
 */
public interface Array extends DataType {

	public static final String ARRAY_LABEL_PREFIX = "ARRAY";

	/**
	 * Returns the number of elements in the array
	 * @return the number of elements in the array
	 */
	int getNumElements();

	/**
	 * Returns the length of an element in the array.  In the case
	 * of a Dynamic base datatype, this element length will have been explicitly specified
	 * at the time of construction.  For a zero-length base type an element length of 1 
	 * will be reported with {@link #getLength()} returning the number of elements.
	 * @return the length of one element in the array.
	 */
	int getElementLength();

	/**
	 * Returns the dataType of the elements in the array.
	 * @return the dataType of the elements in the array
	 */
	DataType getDataType();


	/**
	 * Get the value Class of a specific arrayDt with settings
	 * ( see {@link #getArrayValueClass(Settings)} ).
	 * 
	 * @param settings the relevant settings to use or null for default.
	 * @return Class of the value to be returned by the array or null if it can vary
	 * or is unspecified (String or Array class will be returned).
	 */
	default public Class<?> getArrayValueClass(Settings settings) {
		DataType dt = getDataType();
		if (dt instanceof TypeDef) {
			dt = ((TypeDef) dt).getBaseDataType();
		}
		if (dt instanceof ArrayStringable) {
			if (((ArrayStringable) dt).hasStringValue(settings)) {
				return String.class;
			}
		}
		Class<?> valueClass = dt.getValueClass(settings);
		return valueClass != null ? Array.class : null;
	}

}
