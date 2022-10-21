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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class DataTypeUtilities {
	private static Map<String, DataType> cPrimitiveNameMap = new HashMap<>();


	private static final Pattern DATATYPE_CONFLICT_PATTERN =
		Pattern.compile(Pattern.quote(DataType.CONFLICT_SUFFIX) + "_?[0-9]*");




	/**
	 * Check to see if the second data type is the same as the first data type or is part of it.
	 * <br>
	 * Note: pointers to the second data type are references and therefore are not considered to be
	 * part of the first and won't cause true to be returned. If you pass a pointer to this method
	 * for the first or second parameter, it will return false.
	 * 
	 * @param firstDataType the data type whose components or base type should be checked to see if
	 *            the second data type is part of it.
	 * @param secondDataType the data type to be checked for in the first data type.
	 * @return true if the second data type is the first data type or is part of it.
	 */
	public static boolean isSecondPartOfFirst(DataType firstDataType, DataType secondDataType) {
		if (firstDataType instanceof Pointer || secondDataType instanceof Pointer) {
			return false;
		}
		if (firstDataType.equals(secondDataType)) {
			return true;
		}
		if (firstDataType instanceof Array) {
			DataType elementDataType = ((Array) firstDataType).getDataType();
			return isSecondPartOfFirst(elementDataType, secondDataType);
		}
		if (firstDataType instanceof TypeDef) {
			DataType innerDataType = ((TypeDef) firstDataType).getDataType();
			return isSecondPartOfFirst(innerDataType, secondDataType);
		}
		if (firstDataType instanceof Composite) {
			Composite compositeDataType = (Composite) firstDataType;
			for (DataTypeComponent dtc : compositeDataType.getDefinedComponents()) {
				DataType dataTypeToCheck = dtc.getDataType();
				if (isSecondPartOfFirst(dataTypeToCheck, secondDataType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if the two dataTypes have the same sourceArchive and the same UniversalID
	 * 
	 * @param dataType1 first data type
	 * @param dataType2 second data type
	 * @return true if types correspond to the same type from a source archive
	 */
	public static boolean isSameDataType(DataType dataType1, DataType dataType2) {
		UniversalID id1 = dataType1.getUniversalID();
		UniversalID id2 = dataType2.getUniversalID();
		if (id1 == null || id2 == null) {
			return false;
		}
		if (!id1.equals(id2)) {
			return false;
		}
		// Same universal id, but to be sure make sure the source archives are the same.
		SourceArchive sourceArchive1 = dataType1.getSourceArchive();
		SourceArchive sourceArchive2 = dataType2.getSourceArchive();
		if (sourceArchive1 == null || sourceArchive2 == null) {
			return false;
		}
		return sourceArchive1.getSourceArchiveID().equals(sourceArchive2.getSourceArchiveID());
	}

	/**
	 * Returns true if the two dataTypes have the same sourceArchive and the same UniversalID OR are
	 * equivalent
	 * 
	 * @param dataType1 first data type (if invoked by DB object or manager, this argument must
	 *            correspond to the DataTypeDB).
	 * @param dataType2 second data type
	 * @return true if types correspond to the same type from a source archive or they are
	 *         equivelent, otherwise false
	 */
	public static boolean isSameOrEquivalentDataType(DataType dataType1, DataType dataType2) {
		// if they contain datatypes that have same ids, then they represent the same dataType
		if (isSameDataType(dataType1, dataType2)) {
			return true;
		}
		// otherwise, check if they are equivalent
		return dataType1.isEquivalent(dataType2);
	}

	/**
	 * Get the name of a data type with all conflict naming patterns removed.
	 * 
	 * @param dataType data type
	 * @param includeCategoryPath if true the category path will be included with its
	 * @return name with without conflict patterns
	 */
	public static String getNameWithoutConflict(DataType dataType, boolean includeCategoryPath) {
		String name = includeCategoryPath ? dataType.getPathName() : dataType.getName();
		return DATATYPE_CONFLICT_PATTERN.matcher(name).replaceAll("");
	}

	/**
	 * Compares two data type name strings to determine if they are equivalent names, ignoring
	 * conflict patterns present.
	 * 
	 * @param name1 the first name
	 * @param name2 the second name
	 * @return true if the names are equivalent when conflict suffixes are ignored.
	 */
	public static boolean equalsIgnoreConflict(String name1, String name2) {
		name1 = DATATYPE_CONFLICT_PATTERN.matcher(name1).replaceAll("");
		name2 = DATATYPE_CONFLICT_PATTERN.matcher(name2).replaceAll("");
		return name1.equals(name2);
	}

	/**
	 * Get the base data type for the specified data type stripping away pointers and arrays only. A
	 * null will be returned for a default pointer.
	 *
	 * @param dt the data type whose base data type is to be determined.
	 * @return the base data type.
	 */
	public static DataType getBaseDataType(DataType dt) {
		DataType baseDataType = dt;
		while ((baseDataType instanceof Pointer) || (baseDataType instanceof Array)) {
			if (baseDataType instanceof Pointer) {
				baseDataType = ((Pointer) baseDataType).getDataType();
			}
			else if (baseDataType instanceof Array) {
				baseDataType = ((Array) baseDataType).getDataType();
			}
		}
		return baseDataType;
	}

	public static DataType getArrayBaseDataType(Array arrayDt) {
		DataType dataType = arrayDt.getDataType();
		if (dataType instanceof Array) {
			return getArrayBaseDataType((Array) dataType);
		}
		return dataType;
	}

	private static int getArrayBaseElementLength(Array arrayDt) {
		DataType dataType = arrayDt.getDataType();
		if (dataType instanceof Array) {
			return getArrayBaseElementLength((Array) dataType);
		}
		return arrayDt.getElementLength();
	}

	private static String getArrayElementLengthForDynamic(Array arrayDt) {
		if (getArrayBaseDataType(arrayDt).getLength() <= 0) {
			return " {" + getArrayBaseElementLength(arrayDt) + "} ";
		}
		return "";
	}

	private static String getArrayDimensions(Array arrayDt) {
		String dimensionString = "[" + arrayDt.getNumElements() + "]";
		DataType dataType = arrayDt.getDataType();
		if (dataType instanceof Array) {
			dimensionString += getArrayDimensions((Array) dataType);
		}
		return dimensionString;
	}

	public static String getName(Array arrayDt, boolean showBaseSizeForDynamics) {
		StringBuilder buf = new StringBuilder();
		buf.append(getArrayBaseDataType(arrayDt).getName());
		if (showBaseSizeForDynamics) {
			buf.append(getArrayElementLengthForDynamic(arrayDt));
		}
		buf.append(getArrayDimensions(arrayDt));
		return buf.toString();
	}

	public static String getDisplayName(Array arrayDt, boolean showBaseSizeForDynamics) {
		StringBuilder buf = new StringBuilder();
		buf.append(getArrayBaseDataType(arrayDt).getDisplayName());
		if (showBaseSizeForDynamics) {
			buf.append(getArrayElementLengthForDynamic(arrayDt));
		}
		buf.append(getArrayDimensions(arrayDt));
		return buf.toString();
	}

	public static String getMnemonic(Array arrayDt, boolean showBaseSizeForDynamics,
			Settings settings) {
		StringBuilder buf = new StringBuilder();
		buf.append(getArrayBaseDataType(arrayDt).getMnemonic(settings));
		if (showBaseSizeForDynamics) {
			buf.append(getArrayElementLengthForDynamic(arrayDt));
		}
		buf.append(getArrayDimensions(arrayDt));
		return buf.toString();
	}

	/**
	 * Create a data type category path derived from the specified namespace and rooted from the
	 * specified baseCategory
	 * 
	 * @param baseCategory category path from which to root the namespace-base path
	 * @param namespace the namespace
	 * @return namespace derived category path
	 */


	/**
	 * Attempt to find the data type whose dtName and specified namespace match a stored data type
	 * within the specified dataTypeManager. The best match will be returned. The namespace will be
	 * used in checking data type parent categories, however if no type corresponds to the namespace
	 * another type whose name matches may be returned.
	 * 
	 * @param dataTypeManager data type manager
	 * @param namespace namespace associated with dtName (null indicates no namespace constraint)
	 * @param dtName name of data type
	 * @param classConstraint optional data type interface constraint (e.g., Structure), or null
	 * @return best matching data type
	 */


	/**
	 * Attempt to find the data type whose dtNameWithNamespace match a stored data type within the
	 * specified dataTypeManager. The best match will be returned. The namespace will be used in
	 * checking data type parent categories, however if no type corresponds to the namespace another
	 * type whose name matches may be returned. NOTE: name parsing assumes :: delimiter and can be
	 * thrown off if name include template information which could contain namespaces.
	 * 
	 * @param dataTypeManager data type manager
	 * @param dtNameWithNamespace name of data type qualified with namespace (e.g.,
	 *            ns1::ns2::dtname)
	 * @param classConstraint optional data type interface constraint (e.g., Structure), or null
	 * @return best matching data type
	 */


	/**
	 * Return the appropriate datatype for a given C primitive datatype name.
	 * 
	 * @param dataTypeName the datatype name (e.g. "unsigned int", "long long")
	 * @return the appropriate datatype for a given C primitive datatype name.
	 */

	/**
	 * <code>NamespaceMatcher</code> is used to check data type categoryPath for match against
	 * preferred namespace.
	 */
	private static interface NamespaceMatcher {
		boolean isNamespaceCategoryMatch(DataType dataType);
	}

	private static DataType findDataType(DataTypeManager dataTypeManager, String dtName,
			Class<? extends DataType> classConstraint, NamespaceMatcher preferredCategoryMatcher) {
		ArrayList<DataType> list = new ArrayList<>();
		dataTypeManager.findDataTypes(dtName, list);
		if (!list.isEmpty()) {
			//use the datatype that exists in the root category,
			//otherwise just pick the first one...
			DataType anyDt = null;
			DataType preferredDataType = null;
			for (DataType existingDT : list) {
				if (classConstraint != null &&
					!classConstraint.isAssignableFrom(existingDT.getClass())) {
					continue;
				}
				if (preferredCategoryMatcher == null) {
					if (existingDT.getCategoryPath().equals(CategoryPath.ROOT)) {
						return existingDT;
					}
				}
				if (preferredCategoryMatcher.isNamespaceCategoryMatch(existingDT)) {
					preferredDataType = existingDT;
				}
				// If all else fails return any matching name for backward compatibility
				anyDt = existingDT;
			}
			if (preferredDataType != null) {
				return preferredDataType;
			}
			return anyDt;
		}
		return null;
	}
}
