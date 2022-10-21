/* ###
 * IP: GHIDRA
 * REVIEWED: YES
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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class Utils {

	public static int toUnsignedInt(short x) {
		return x & '\uffff';
	}

	public static int compareUnsigned(short x, short y) {
		return toUnsignedInt(x) - toUnsignedInt(y);
	}

	public static InputStream nullInputStream() {
		return new InputStream() {
			private volatile boolean closed;

			private void ensureOpen() throws IOException {
				if (this.closed) {
					throw new IOException("Stream closed");
				}
			}

			public int available() throws IOException {
				this.ensureOpen();
				return 0;
			}

			public int read() throws IOException {
				this.ensureOpen();
				return -1;
			}

			public int read(byte[] b, int off, int len) throws IOException {
				//Objects.checkFromIndexSize(off, len, b.length);
				if (len == 0) {
					return 0;
				} else {
					this.ensureOpen();
					return -1;
				}
			}

			public byte[] readAllBytes() throws IOException {
				this.ensureOpen();
				return new byte[0];
			}

			public int readNBytes(byte[] b, int off, int len) throws IOException {
				//Objects.checkFromIndexSize(off, len, b.length);
				this.ensureOpen();
				return 0;
			}

			public byte[] readNBytes(int len) throws IOException {
				if (len < 0) {
					throw new IllegalArgumentException("len < 0");
				} else {
					this.ensureOpen();
					return new byte[0];
				}
			}

			public long skip(long n) throws IOException {
				this.ensureOpen();
				return 0L;
			}

			public long transferTo(OutputStream out) throws IOException {
				Objects.requireNonNull(out);
				this.ensureOpen();
				return 0L;
			}

			public void close() throws IOException {
				this.closed = true;
			}
		};
	}
	
	private static long[] uintbmasks = {
			0, 0xff, 0xffff, 0xffffff, 0xffffffffL, 0xffffffffffL, 0xffffffffffffL,
			0xffffffffffffffL, 0xffffffffffffffffL
	};

	public static long calc_mask( int size ) {
		return uintbmasks[(size < 8) ? size : 8];
	}

	public static long pcode_right( long val, int sa ) {
		if ( sa >= 64 ) {
			return 0;
		}
		return val >>> sa;
	}

	public static long pcode_left( long val, int sa ) {
		if ( sa >= 64 ) {
			return 0;
		}
		return val << sa;
	}

	// ostream &operator<<(ostream &s,const cpui_seqnum &sq)
	//
	// {
	// sq.pc.printraw(s);
	// s << ':' << sq.uniq;
	// return s;
	// }
	//
	// ostream &operator<<(ostream &s,const cpui_mach_addr &addr)
	//
	// {
	// addr.printraw(s);
	// return s;
	// }

	public static boolean signbit_negative( long val, int size ) { // Return true if signbit is set
																	// (negative)
		long mask = 0x80;
		mask <<= 8 * (size - 1);
		return ((val & mask) != 0);
	}

	public static long uintb_negate( long in, int size ) { // Invert bits
		return ((~in) & calc_mask( size ));
	}

	public static long sign_extend( long in, int sizein, int sizeout )

	{
		int signbit;
		long mask;

		signbit = sizein * 8 - 1;
		in &= calc_mask( sizein );
		if ( sizein >= sizeout ) {
			return in;
		}
		if ( (in >>> signbit) != 0 ) {
			mask = calc_mask( sizeout );
			long tmp = mask << signbit; // Split shift into two pieces
			tmp = (tmp << 1) & mask; // In case, everything is shifted out
			in |= tmp;
		}
		return in;
	}

	// this used to void and changed the parameter val - can't do it in java
	public static long zzz_sign_extend( long val, int bit )

	{ // Sign extend -val- above -bit-
		long mask = 0;
		mask = (~mask) << bit;
		if ( ((val >>> bit) & 1) != 0 ) {
			val |= mask;
		} else {
			val &= (~mask);
		}
		return val;
	}

	// this used to void and changed the parameter val - can't do it in java
	public static long zzz_zero_extend( long val, int bit ) { // Clear all bits in -val- above
																// -bit-
		long mask = 0;
		mask = (~mask) << bit;
		mask <<= 1;
		val &= (~mask);
		return val;
	}

	// this used to void and changed the parameter val - can't do it in java
	public static long byte_swap( long val, int size ) { // Swap the least sig -size- bytes in val
		long res = 0;
		while ( size > 0 ) {
			res <<= 8;
			res |= (val & 0xff);
			val >>>= 8;
			size -= 1;
		}
		return res;
	}

	long byte_swap( int val ) { // Swap the bytes for the whole machine int
		long res = 0;
		for ( int i = 0; i < 4; ++i ) {
			res <<= 8;
			res |= (val & 0xff);
			val >>>= 8;
		}
		return res;
	}

    public static boolean isprint(int c) {
        if (c >= 32 && c <= 126)
            return true;
        return false;
    }

    public static boolean isascii(int c) {
        if (c >= 0 && c <= 127)
            return true;
        return false;
    }

    public static int leastsigbit_set( long val ) { // Return bit number (0=lsb)
		// of the least signifigant bit set or -1 if none set
		if ( val == 0 ) {
			return -1;
		}
		int res = 0;
		int sz = 32;
		long mask = -1;
		do {
			mask >>>= sz;
			if ( (mask & val) == 0 ) {
				res += sz;
				val >>>= sz;
			}
			sz >>= 1;
		} while ( sz != 0 );
		return res;
	}

	public static int mostsigbit_set( long val ) { // Return bit number (0=lsb)
		// of the most signifigant bit set or -1 if none set
		if ( val == 0 ) {
			return -1;
		}

		int res = 63;
		int sz = 32;
		long mask = -1;
		do {
			mask <<= sz;
			if ( (mask & val) == 0 ) {
				res -= sz;
				val <<= sz;
			}
			sz >>= 1;
		} while ( sz != 0 );
		return res;
	}

	public static long coveringmask( long val )

	{ // Return smallest number of form 2^n-1, bigger or equal to val
		long res = val;
		int sz = 1;
		while ( sz < 64 ) {
			res = res | (res >>> sz);
			sz <<= 1;
		}
		return res;
	}

	public static String paddedHexString( long value, int padLength ) {
		String decodedString = Long.toString( value, 16 );
		if ( decodedString.length() >= padLength ) {
			return decodedString;
		}

		StringBuffer buffer = new StringBuffer();
		int missingLength = padLength - decodedString.length();
		for ( int i = 0; i < missingLength; i++ ) {
			buffer.append( "0" );
		}
		buffer.append( decodedString );
		return buffer.toString();
	}

	public static int unsignedCompare( long v1, long v2 ) {
		if ( v1 == v2 ) {
			return 0;
		}
		if ( v1 >= 0 && v2 >= 0 ) {
			return v1 < v2 ? -1 : 1;
		}
		if ( v1 < 0 && v2 < 0 ) {
			return v1 < v2 ? -1 : 1;
		}
		if ( v1 < 0 ) {
			return 1;
		}
		return -1;
	}
	public static int unsignedCompare( int v1, int v2 ) {
		if ( v1 == v2 ) {
			return 0;
		}
		if ( v1 >= 0 && v2 >= 0 ) {
			return v1 < v2 ? -1 : 1;
		}
		if ( v1 < 0 && v2 < 0 ) {
			return v1 < v2 ? -1 : 1;
		}
		if ( v1 < 0 ) {
			return 1;
		}
		return -1;
	}

	public static int bytesToInt( byte[] bytes, boolean bigEndian ) {
		int result = 0;
		if (bigEndian) {
			for(int i=0;i<4;i++) {
				result <<= 8;
				result |= (bytes[i] & 0xFF);
			}
		}
		else {
			for(int i=3;i>=0;i--) {
				result <<= 8;
				result |= (bytes[i] & 0xFF);
			}
			
		}
		return result;
	}
	
	public static long shiftLeft(long a, long b) {
		b &= 0xff;
		if (b >= 64) {
			return 0;
		}
		return a << b;
	}
	public static long ashiftRight(long a, long b) {
		b &= 0xff;
		if (b >= 64) {
			return -1;
		}
		return a >> b;
	}
	public static long lshiftRight(long a, long b) {
		b &= 0xff;
		if (b >= 64) {
			return 0;
		}
		return a >>> b;
	}
	public static long unsignedInt(int a) {
	    long result = a;
	    if (result < 0) {
	        result += 4294967296L;
	    }
	    return result;
	}
    public static int unsignedDivide(int a, int b) {
        long la = unsignedInt(a);
        long lb = unsignedInt(b);
        long result = la/lb;
        return (int) result;
    }
    public static int unsignedModulo(int a, int b) {
        long la = unsignedInt(a);
        long lb = unsignedInt(b);
        long result = la%lb;
        return (int) result;
    }
	public static void main(String[] args) {
        System.out.println(unsignedDivide(-8, 32));
        System.out.println(unsignedModulo(-8, 32));
	}
	public static String toUnsignedIntHex(int n) {
	    return Long.toHexString(unsignedInt(n));
	}

	public static long bytesToLong(byte[] byteBuf) {
		long value = 0;
		for(int i=0;i<8;i++) {
			value = value << 8 | byteBuf[i];
		}
		return value;
	}
}
