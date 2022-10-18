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
 * <code>ElfLoadHelper</code> exposes loader methods useful to ElfExtension 
 * implementations.
 */
public interface ElfLoadHelper {

	/**
	 * Get program object
	 * @return program object
	 */
	Program getProgram();

	/**
	 * Get ELF Header object
	 * @return ELF Header object
	 */
	ElfHeader getElfHeader();



	/**
	 * Output loader log message
	 * @param msg text message
	 */
	void log(String msg);

	/**
	 * Output loader log message.
	 * @param t exception/throwable error
	 */
	void log(Throwable t);

	/**
	 * Mark this location as code in the CodeMap.
	 * The analyzers will pick this up and disassemble the code.
	 * @param address
	 */
	void markAsCode(Address address);



	/**
	 * Create an undefined data item to reserve the location as data, without specifying the type
	 * @param address  location of undefined data to create
	 * @param length  size of the undefined data item
	 */
	Data createUndefinedData(Address address, int length);

	/**
	 * Create a data item using the specified data type
	 * @param address  location of undefined data to create
	 * @param dt data type
	 * @return data or null if not successful
	 */
	Data createData(Address address, DataType dt);

	/**
	 * Add specified elfSymbol to the loader symbol map after its program address has been assigned
	 * @param elfSymbol elf symbol
	 * @param address program address (may be null if not applicable)
	 */
	void setElfSymbolAddress(ElfSymbol elfSymbol, Address address);

	/**
	 * Get the memory address of a previously resolved symbol
	 * @param elfSymbol elf symbol
	 * @return memory address or null if unknown
	 */
	Address getElfSymbolAddress(ElfSymbol elfSymbol);


	/**
	 * Get the program address for an addressableWordOffset within the default address space.  
	 * This method is responsible for applying any program image base change imposed during 
	 * the import (see {@link #getImageBaseWordAdjustmentOffset()}.
	 * @param addressableWordOffset absolute word offset.  The offset should already include
	 * default image base and pre-link adjustment (see {@link ElfHeader#adjustAddressForPrelink(long)}).  
	 * @return memory address in default code space
	 */
	Address getDefaultAddress(long addressableWordOffset);

	/**
	 * Get the program image base offset adjustment.  The value returned reflects the
	 * actual program image base minus the default image base (see {@link ElfHeader#getImageBase()}.
	 * This will generally be zero (0), unless the program image base differs from the
	 * default.  It may be necessary to add this value to any pre-linked address values
	 * such as those contained with the dynamic table. (Applies to default address space only)
	 * @return image base adjustment value
	 */
	public long getImageBaseWordAdjustmentOffset();

	/**
	 * Returns the appropriate .got (Global Offset Table) section address using the
	 * DT_PLTGOT value defined in the .dynamic section.
	 * If the dynamic value is not defined, the symbol offset for _GLOBAL_OFFSET_TABLE_
	 * will be used, otherwise null will be returned.
	 * @return the .got section address offset
	 */
	public Long getGOTValue();



	/**
	 * <p>Get the original memory value at the specified address if a relocation was applied at the
	 * specified address (not containing).  Current memory value will be returned if no relocation
	 * has been applied at specified address.  The value size is either 8-bytes if {@link ElfHeader#is64Bit()},
	 * otherwise it will be 4-bytes.  This is primarily intended to inspect original bytes within 
	 * the GOT which may have had relocations applied to them.
	 * @param addr memory address
	 * @param signExtend if true sign-extend to long, else treat as unsigned
	 * @return original bytes value
	 * @throws MemoryAccessException if memory read fails
	 */
	public long getOriginalValue(Address addr, boolean signExtend)
			throws MemoryAccessException;

}
