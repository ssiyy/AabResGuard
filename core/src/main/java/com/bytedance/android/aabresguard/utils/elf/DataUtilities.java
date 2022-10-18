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

public final class DataUtilities {

	private DataUtilities() {
		// utilities class
	}

	/**
	 * Determine if the specified name is a valid data-type name
	 * @param name candidate data-type name
	 * @return true if name is valid, else false
	 */
	public static boolean isValidDataTypeName(String name) {
		if (name == null || name.length() == 0) {
			return false;
		}

		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			// Don't allow control characters, but otherwise accept as much as possible
			//   a) allow spaces and punctuation
			//   b) allow unicode characters (including supplemental characters)
			if (Character.isISOControl(c)) {
				return false;
			}
		}

		return true;
	}


}
