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

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FSUtilities {



	private static char[] hexdigit = "0123456789abcdef".toCharArray();

	/**
	 * Returns a copy of the input string with FSRL problematic[1] characters escaped
	 * as "%nn" sequences, where nn are hexdigits specifying the numeric ascii value
	 * of that character.
	 * <p>
	 * Characters that need more than a byte to encode will result in multiple "%nn" values
	 * that encode the necessary UTF8 codepoints.
	 * <p>
	 * [1] - non-ascii / unprintable / FSRL portion separation characters.
	 *
	 * @param s string, or null.
	 * @return string with problematic characters escaped as "%nn" sequences, or null
	 * if parameter was null.
	 */
	public static String escapeEncode(String s) {
		if (s == null) {
			return null;
		}

		String escapeChars = "%?|";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 32 || c > 126 || escapeChars.indexOf(c) >= 0) {
				appendHexEncoded(sb, c);
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Returns a decoded version of the input stream where "%nn" escape sequences are
	 * replaced with their actual characters, using UTF-8 decoding rules.
	 * <p>
	 *
	 * @param s string with escape sequences in the form "%nn", or null.
	 * @return string with all escape sequences replaced with native characters, or null if
	 * original parameter was null.
	 * @throws MalformedURLException if bad escape sequence format.
	 */
	public static String escapeDecode(String s) throws MalformedURLException {
		if (s == null) {
			return null;
		}

		byte[] bytes = null;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length();) {
			char c = s.charAt(i);
			if (c == '%') {
				if (bytes == null) {
					bytes = new byte[(s.length() - i) / 3];
				}
				int pos = 0;

				while (((i + 2) < s.length()) && (c == '%')) {
					int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
					if (v < 0) {
						throw new MalformedURLException(
							"Bad hex characters in escape (%) pattern: " + s);
					}
					bytes[pos++] = (byte) v;
					i += 3;
					if (i < s.length()) {
						c = s.charAt(i);
					}
				}

				if ((i < s.length()) && (c == '%')) {
					throw new MalformedURLException("Bad escape pattern in " + s);
				}

				sb.append(new String(bytes, 0, pos, StandardCharsets.UTF_8));
			}
			else {
				sb.append(c);
				i++;
			}
		}
		return sb.toString();
	}

	private static void appendHexEncoded(StringBuilder sb, char c) {
		if (c < 0x80) {
			sb.append('%').append(hexdigit[c >> 4]).append(hexdigit[c & 0x0f]);
			return;
		}
//todo --------------------
		sb.append(URLEncoder.encode("" + c));
	}





	/**
	 * Concats path strings together, taking care to ensure that there is a correct
	 * path separator character between each part.
	 * <p>
	 * Handles forward or back slashes as path separator characters in the input, but
	 * only adds forward slashes when separating the path strings that need a separator.
	 * <p>
	 * @param paths vararg list of path strings, empty or null elements are ok and are skipped.
	 * @return null if all params null, "" empty string if all are empty, or
	 * "path_element[1]/path_element[2]/.../path_element[N]" otherwise.
	 */
	public static String appendPath(String... paths) {
		if ((CollectionUtils.isAllNull(Arrays.asList(paths)))) {
			return null;
		}

		StringBuilder buffer = new StringBuilder();
		for (String path : paths) {
			if (path == null || path.isEmpty()) {
				continue;
			}

			boolean emptyBuffer = buffer.length() == 0;
			boolean bufferEndsWithSlash =
				!emptyBuffer && "/\\".indexOf(buffer.charAt(buffer.length() - 1)) != -1;
			boolean pathStartsWithSlash = "/\\".indexOf(path.charAt(0)) != -1;

			if (!bufferEndsWithSlash && !pathStartsWithSlash && !emptyBuffer) {
				buffer.append("/");
			}
			else if (pathStartsWithSlash && bufferEndsWithSlash) {
				path = path.substring(1);
			}
			buffer.append(path);
		}

		return buffer.toString();
	}


}
