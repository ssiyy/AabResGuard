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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


/**
 * <code>ElfLoadAdapter</code> provides the base ELF load adapter implementation 
 * which may be extended to facilitate target specific behavior.
 */
public class ElfLoadAdapter {

	/**
	 * Add all extension specific Dynamic table entry types (e.g., DT_ prefix).
	 * This method will add all those statically defined ElfDynamicType fields
	 * within this class.
	 * @param dynamicTypeMap map to which ElfDynamicType definitions should be added
	 */
	public final void addDynamicTypes(Map<Integer, ElfDynamicType> dynamicTypeMap) {

		for (Field field : getClass().getDeclaredFields()) {
			String name = null;
			try {
				if (Modifier.isStatic(field.getModifiers()) &&
					field.getType().equals(ElfDynamicType.class)) {
					ElfDynamicType type = (ElfDynamicType) field.get(this);
					name = type.name;
					ElfDynamicType.addDynamicType(type, dynamicTypeMap);
				}
			}
			catch (DuplicateNameException e) {
			//	Msg.error(this,"Invalid ElfDynamicType(" + name + ") defined by " + getClass().getName(), e);
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				throw new AssertException(e);
			}catch (Exception e){

			}
		}
	}

	/**
	 * Add all extension specific Program Header types (e.g., PT_ prefix).
	 * This method will add all those statically defined ElfProgramHeaderType fields
	 * within this class.
	 * @param programHeaderTypeMap map to which ElfProgramHeaderType definitions should be added
	 */
	public final void addProgramHeaderTypes(
			Map<Integer, ElfProgramHeaderType> programHeaderTypeMap) {

		for (Field field : getClass().getDeclaredFields()) {
			String name = null;
			try {
				if (Modifier.isStatic(field.getModifiers()) &&
					field.getType().equals(ElfProgramHeaderType.class)) {
					ElfProgramHeaderType type = (ElfProgramHeaderType) field.get(this);
					name = type.name;
					ElfProgramHeaderType.addProgramHeaderType(type, programHeaderTypeMap);
				}
			}
			catch (DuplicateNameException e) {
				//Msg.error(this,"Invalid ElfProgramHeaderType(" + name + ") defined by " + getClass().getName(),	e);
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				throw new AssertException(e);
			}catch (Exception e){

			}
		}
	}

	/**
	 * Add all extension specific Section Header types (e.g., SHT_ prefix).
	 * This method will add all those statically defined ElfSectionHeaderType fields
	 * within this class.
	 * @param sectionHeaderTypeMap map to which ElfSectionHeaderType definitions should be added
	 */
	public final void addSectionHeaderTypes(
			HashMap<Integer, ElfSectionHeaderType> sectionHeaderTypeMap) {

		for (Field field : getClass().getDeclaredFields()) {
			String name = null;
			try {
				if (Modifier.isStatic(field.getModifiers()) &&
					field.getType().equals(ElfSectionHeaderType.class)) {
					ElfSectionHeaderType type = (ElfSectionHeaderType) field.get(this);
					name = type.name;
					ElfSectionHeaderType.addSectionHeaderType(type, sectionHeaderTypeMap);
				}
			}
			catch (DuplicateNameException e) {
			//	Msg.error(this,					"Invalid ElfSectionHeaderType(" + name + ") defined by " + getClass().getName(),					e);
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				throw new AssertException(e);
			}catch (Exception e){

			}
		}
	}

	/**
	 * Check if this extension can handle the specified elf header.  If this method returns 
	 * true, this extension will be used to obtain extended types definitions and to perform
	 * additional load processing.
	 * @param elf elf header
	 * @return true if this extension should be used when loading the elf image which
	 * corresponds to the specified header.
	 */
	public boolean canHandle(ElfHeader elf) {
		return false;
	}



	/**
	 * Return the data type naming suffix which should be used when creating types derived 
	 * from data supplied by this extension.
	 * @return type naming suffix or null
	 */
	public String getDataTypeSuffix() {
		return null;
	}



	/**
	 * Get the write permission for the specified section.
	 * @param section section header object
	 * @return true if write enabled, else false or null to use standard Elf section
	 * flags to make the determination.
	 */
	public Boolean isSectionWritable(ElfSectionHeader section) {
		return (section.getFlags() & ElfSectionHeaderConstants.SHF_WRITE) != 0;
	}

	/**
	 * Get the execute permission for the specified section (i.e., instructions permitted).
	 * @param section section header object
	 * @return true if execute enabled, else false or null to use standard Elf section
	 * flags to make the determination.
	 */
	public Boolean isSectionExecutable(ElfSectionHeader section) {
		return (section.getFlags() & ElfSectionHeaderConstants.SHF_EXECINSTR) != 0;
	}

	/**
	 * Determine if the specified section is "allocated" within memory.
	 * @param section section header object
	 * @return true if section should be allocated, else false or null to use standard Elf section
	 * flags to make the determination.
	 */
	public Boolean isSectionAllocated(ElfSectionHeader section) {
		return (section.getFlags() & ElfSectionHeaderConstants.SHF_ALLOC) != 0;
	}

	/**
	 * Get the dynamic memory block allocation alignment as addressable units
	 * within the default memory space.
	 * @return dynamic memory block allocation alignment.
	 */
	public int getLinkageBlockAlignment() {
		return 0x1000; // 4K alignment
	}

	/**
	 * Get the preferred free range size for the EXTERNAL memory block as addressable units
	 * within the default memory space.
	 * @return minimum free range size for EXTERNAL memory block as addressable units
	 */
	public int getPreferredExternalBlockSize() {
		return 0x20000; // 128K
	}

	/**
	 * Get reserve size of the EXTERNAL memory block as addressable units
	 * within the default memory space.  This size represents the largest 
	 * expansion size to the block which could occur during relocation
	 * processing.
	 * @return reserve size of the EXTERNAL memory block as addressable units
	 */
	public int getExternalBlockReserveSize() {
		return 0x10000; // 64K
	}

	/**
	 * Return the memory section size in bytes for the specified section header.
	 * The returned value will be consistent with any byte filtering which may be required.
	 * @param section the section header
	 * @return preferred memory block size in bytes which corresponds to the specified section header
	 */
	public long getAdjustedSize(ElfSectionHeader section) {
		return section.getSize();
	}

	/**
	 * Get the ElfRelocation class which should be used to properly parse
	 * the relocation tables.
	 * @param elfHeader ELF header object (for header field access only)
	 * @return ElfRelocation class or null for default behavior
	 */
	public Class<? extends ElfRelocation> getRelocationClass(ElfHeader elfHeader) {
		return null;
	}

	public long getAdjustedMemorySize(ElfProgramHeader elfProgramHeader) {
		return elfProgramHeader.getMemorySize();
	}

	/**
	 * Return the memory bytes to be loaded from the underlying file for the specified program header.
	 * The returned value will be consistent with any byte filtering which may be required.
	 * @param elfProgramHeader
	 * @return preferred memory block size in bytes which corresponds to the specified program header
	 */
	public long getAdjustedLoadSize(ElfProgramHeader elfProgramHeader) {
		return elfProgramHeader.getFileSize();
	}
}
