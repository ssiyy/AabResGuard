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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class with static methods that deal with string manipulation.
 */
public class StringUtilities {

	/**
	 * Create the bi-directional mapping between control characters and escape sequences.
	 */
	private static Map<Character, String> controlToEscapeStringMap = new HashMap<>();
	private static Map<String, Character> escapeStringToControlMap = new HashMap<>();
	static {
		controlToEscapeStringMap.put('\t', "\\t");
		controlToEscapeStringMap.put('\b', "\\b");
		controlToEscapeStringMap.put('\r', "\\r");
		controlToEscapeStringMap.put('\n', "\\n");
		controlToEscapeStringMap.put('\f', "\\f");
		controlToEscapeStringMap.put('\\', "\\\\");
		controlToEscapeStringMap.put((char) 0xb, "\\v");
		controlToEscapeStringMap.put((char) 0x7, "\\a");

		escapeStringToControlMap.put("\\t", '\t');
		escapeStringToControlMap.put("\\b", '\b');
		escapeStringToControlMap.put("\\r", '\r');
		escapeStringToControlMap.put("\\n", '\n');
		escapeStringToControlMap.put("\\f", '\f');
		escapeStringToControlMap.put("\\\\", '\\');
		escapeStringToControlMap.put("\\v", (char) 0xb);
		escapeStringToControlMap.put("\\a", (char) 0x7);

		// this one is special to allow users to enter nulls in search strings - don't
		// want the reverse
		escapeStringToControlMap.put("\\0", '\0');
	}

	private static final String ELLIPSES = "...";

	public static final Pattern DOUBLE_QUOTED_STRING_PATTERN =
		Pattern.compile("^\"((?:[^\\\\\"]|\\\\.)*)\"$");

	/**
	 * The platform specific string that is the line separator.
	 */
	public final static String LINE_SEPARATOR = System.getProperty("line.separator");

	public static final int UNICODE_REPLACEMENT = 0xFFFD;

	/**
	 * Unicode Byte Order Marks (BOM) characters are special characters in the Unicode character
	 * space that signal endian-ness of the text.
	 * <p>
	 * The value for the BigEndian version (0xFEFF) works for both 16 and 32 bit character values.
	 * <p>
	 * There are separate values for Little Endian Byte Order Marks for 16 and 32 bit characters
	 * because the 32 bit value is shifted left by 16 bits.
	 */
	public static final int UNICODE_BE_BYTE_ORDER_MARK = 0xFEFF;
	public static final int UNICODE_LE16_BYTE_ORDER_MARK = 0x0____FFFE;
	public static final int UNICODE_LE32_BYTE_ORDER_MARK = 0xFFFE_0000;

	// This is Java's default rendered size of a tab (in spaces) 
	public static final int DEFAULT_TAB_SIZE = 8;

	private StringUtilities() {
		// utility class; can't create
	}

	/**
	 * Returns true if the given character is a special character. For example a '\n' or '\\'. A
	 * value of 0 is not considered special for this purpose as it is handled separately because it
	 * has more varied use cases.
	 *
	 * @param c the character
	 * @return true if the given character is a special character
	 */
	public static boolean isControlCharacterOrBackslash(char c) {
		return controlToEscapeStringMap.containsKey(c);
	}

	/**
	 * Returns true if the given codePoint (ie. full unicode 32bit character) is a special
	 * character. For example a '\n' or '\\'. A value of 0 is not considered special for this
	 * purpose as it is handled separately because it has more varied use cases.
	 *
	 * @param codePoint the codePoint (ie. character), see {@link String#codePointAt(int)}
	 * @return true if the given character is a special character
	 */
	public static boolean isControlCharacterOrBackslash(int codePoint) {
		return (0 <= codePoint && codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) &&
			isControlCharacterOrBackslash((char) codePoint);
	}

	/**
	 * Determines if a string is enclosed in double quotes (ASCII 34 (0x22))
	 * 
	 * @param str String to test for double-quote enclosure
	 * @return True if the first and last characters are the double-quote character, false otherwise
	 */
	public static boolean isDoubleQuoted(String str) {
		Matcher m = DOUBLE_QUOTED_STRING_PATTERN.matcher(str);
		return m.matches();
	}

	/**
	 * If the given string is enclosed in double quotes, extract the inner text. Otherwise, return
	 * the given string unmodified.
	 * 
	 * @param str String to match and extract from
	 * @return The inner text of a doubly-quoted string, or the original string if not
	 *         double-quoted.
	 */
	public static String extractFromDoubleQuotes(String str) {
		Matcher m = DOUBLE_QUOTED_STRING_PATTERN.matcher(str);
		if (m.matches()) {
			return m.group(1);
		}
		return str;
	}


	/**
	 * Converts the character into a string. If the character is special, it will actually render
	 * the character. For example, given '\n' the output would be "\\n".
	 * 
	 * @param c the character to convert into a string
	 * @return the converted character
	 */
	public static String characterToString(char c) {
		String escaped = controlToEscapeStringMap.get(c);
		if (escaped == null) {
			escaped = Character.toString(c);
		}
		return escaped;
	}

	/**
	 * Returns a count of how many times the 'occur' char appears in the strings.
	 * 
	 * @param string the string to look inside
	 * @param occur the character to look for/
	 * @return a count of how many times the 'occur' char appears in the strings
	 */
	public static int countOccurrences(String string, char occur) {
		int count = 0;
		int length = string.length();
		for (int i = 0; i < length; ++i) {
			if (string.charAt(i) == occur) {
				++count;
			}
		}
		return count;
	}

	public static boolean equals(String s1, String s2, boolean caseSensitive) {
		if (s1 == null) {
			return s2 == null;
		}
		if (caseSensitive) {
			return s1.equals(s2);
		}
		return s1.equalsIgnoreCase(s2);
	}

	public static boolean endsWithWhiteSpace(String string) {
		return Character.isWhitespace(string.charAt(string.length() - 1));
	}

	/**
	 * Generate a quoted string from US-ASCII character bytes assuming 1-byte chars.
	 * <p>
	 * Special characters and non-printable characters will be escaped using C character escape
	 * conventions (e.g., \t, \n, \\uHHHH, etc.). If a character size other than 1-byte is required
	 * the alternate form of this method should be used.
	 * <p>
	 * The result string will be single quoted (ie. "'") if the input byte array is 1 byte long,
	 * otherwise the result will be double-quoted ('"').
	 *
	 * @param bytes character string bytes
	 * @return escaped string for display use
	 */
	public static String toQuotedString(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (byte b : bytes) {
			appendCharConvertedToEscapeSequence((char) (b & 0xff), 1, builder);
		}
		String str = builder.toString();
		if (bytes.length == 1) {
			str = str.replace("'", "\\'");
			return "'" + str + "'";
		}
		str = str.replace("\"", "\\\"");
		return "\"" + str + "\"";
	}

	/**
	 * Generate a quoted string from US-ASCII characters, where each character is charSize bytes.
	 * <p>
	 * Special characters and non-printable characters will be escaped using C character escape
	 * conventions (e.g., \t, \n, \\uHHHH, etc.).
	 * <p>
	 * The result string will be single quoted (ie. "'") if the input byte array is 1 character long
	 * (ie. charSize), otherwise the result will be double-quoted ('"').
	 *
	 * @param bytes array of bytes
	 * @param charSize number of bytes per character (1, 2, 4).
	 * @return escaped string for display use
	 */
	public static String toQuotedString(byte[] bytes, int charSize) {
		if (charSize <= 1) {
			return toQuotedString(bytes);
		}
		else if (charSize > 4) {
			throw new IllegalArgumentException("unsupported charSize: " + charSize);
		}
		int shortage = bytes.length % charSize;
		if (shortage != 0) {
			byte[] paddedBytes = new byte[bytes.length + shortage];
			System.arraycopy(bytes, 0, paddedBytes, 0, bytes.length);
			bytes = paddedBytes;
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < bytes.length; i += charSize) {
			int val = 0;
			for (int n = 0; n < charSize; n++) {
				val = (val << 8) + (bytes[i + n] & 0xff);
			}
			appendCharConvertedToEscapeSequence(val, charSize, builder);
		}
		String str = builder.toString();
		if (bytes.length <= charSize) {
			str = str.replace("'", "\\'");
			return "'" + str + "'";
		}
		str = str.replace("\"", "\\\"");
		return "\"" + str + "\"";
	}

	private static void appendCharConvertedToEscapeSequence(int c, int charSize,
			StringBuilder builder) {
		if (controlToEscapeStringMap.containsKey((char) c)) {
			builder.append(controlToEscapeStringMap.get((char) c));
		}
		else if (c >= 0x20 && c <= 0x7f) {
			builder.append((char) c);
		}
		else if (charSize <= 1) {
			builder.append("\\x" + pad(Integer.toHexString(c), '0', 2));
		}
		else if (charSize == 2) {
			builder.append("\\u" + pad(Integer.toHexString(c), '0', 4));
		}
		else if (charSize <= 4) {
			builder.append("\\U" + pad(Integer.toHexString(c), '0', 8));
		}
		else {
			// TODO: unsupported
		}
	}

	/**
	 * Returns true if the given string starts with <code>prefix</code> ignoring case.
	 * <p>
	 * Note: This method is equivalent to calling:
	 * 
	 * <pre>
	 * string.regionMatches(true, 0, prefix, 0, prefix.length());
	 * </pre>
	 *
	 * @param string the string which may contain the prefix
	 * @param prefix the prefix to test against
	 * @return true if the given string starts with <code>prefix</code> ignoring case.
	 */
	public static boolean startsWithIgnoreCase(String string, String prefix) {
		if ((string == null) || (prefix == null)) {
			return false;
		}
		return string.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	/**
	 * Returns true if the given string ends with <code>postfix</code>, ignoring case.
	 * <p>
	 * Note: This method is equivalent to calling:
	 * 
	 * <pre>
	 * int startIndex = string.length() - postfix.length();
	 * string.regionMatches(true, startOffset, postfix, 0, postfix.length());
	 * </pre>
	 *
	 * @param string the string which may end with <code>postfix</code>
	 * @param postfix the string for which to test existence
	 * @return true if the given string ends with <code>postfix</code>, ignoring case.
	 */
	public static boolean endsWithIgnoreCase(String string, String postfix) {
		if ((string == null) || (postfix == null)) {
			return false;
		}
		int startIndex = string.length() - postfix.length();
		return string.regionMatches(true, startIndex, postfix, 0, postfix.length());
	}


	/**
	 * Returns the index of the first whole word occurrence of the search word within the given
	 * text. A whole word is defined as the character before and after the occurrence must not be a
	 * JavaIdentifierPart.
	 * 
	 * @param text the text to be searched.
	 * @param searchWord the word to search for.
	 * @return the index of the first whole word occurrence of the search word within the given
	 *         text, or -1 if not found.
	 */
	public static int indexOfWord(String text, String searchWord) {
		int index = 0;
		while (index < text.length()) {
			index = text.indexOf(searchWord, index);
			if (index < 0) {
				return -1;
			}
			if (isWholeWord(text, index, searchWord.length())) {
				return index;
			}
			index += searchWord.length();
		}
		return -1;
	}

	/**
	 * Returns true if the substring within the text string starting at startIndex and having the
	 * given length is a whole word. A whole word is defined as the character before and after the
	 * occurrence must not be a JavaIdentifierPart.
	 * 
	 * @param text the text containing the potential word.
	 * @param startIndex the start index of the potential word within the text.
	 * @param length the length of the potential word
	 * @return true if the substring within the text string starting at startIndex and having the
	 *         given length is a whole word.
	 */
	public static boolean isWholeWord(String text, int startIndex, int length) {
		if (startIndex > 0) {
			if (Character.isJavaIdentifierPart(text.charAt(startIndex - 1))) {
				return false;
			}
		}
		int endIndex = startIndex + length;
		if (endIndex < text.length()) {
			if (Character.isJavaIdentifierPart(text.charAt(endIndex))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Convert tabs in the given string to spaces using a default tab width of 8 spaces.
	 *
	 * @param str string containing tabs
	 * @return string that has spaces for tabs
	 */
	public static String convertTabsToSpaces(String str) {
		return convertTabsToSpaces(str, DEFAULT_TAB_SIZE);
	}

	/**
	 * Convert tabs in the given string to spaces.
	 *
	 * @param str string containing tabs
	 * @param tabSize length of the tab
	 * @return string that has spaces for tabs
	 */
	public static String convertTabsToSpaces(String str, int tabSize) {
		if (str == null) {
			return null;
		}

		StringBuffer buffer = new StringBuffer();

		int linepos = 0;

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '\t') {
				int nSpaces = tabSize - (linepos % tabSize);
				String pad = pad("", ' ', nSpaces);
				buffer.append(pad);
				linepos += nSpaces;
			}
			else {
				buffer.append(c);
				linepos++;
				if (c == '\n') {
					linepos = 0;
				}
			}
		}

		return buffer.toString();
	}


	/**
	 * Pads the source string to the specified length, using the filler string as the pad. If length
	 * is negative, left justifies the string, appending the filler; if length is positive, right
	 * justifies the source string.
	 *
	 * @param source the original string to pad.
	 * @param filler the type of characters with which to pad
	 * @param length the length of padding to add (0 results in no changes)
	 * @return the padded string
	 */
	public static String pad(String source, char filler, int length) {

		if (length == 0) {
			return source;
		}

		boolean rightJustify = true;
		if (length < 0) {
			rightJustify = false;
			length *= -1;
		}

		int n = length - source.length();
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < n; i++) {
			buffer.append(filler);
		}

		if (rightJustify) {
			buffer.append(source);
		}
		else {
			buffer.insert(0, source);
		}

		return buffer.toString();
	}


}
