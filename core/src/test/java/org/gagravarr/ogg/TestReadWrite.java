/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gagravarr.ogg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Test that we can open a file, read it in, write it
 *  back out again, and not break anything in
 *  the process!
 */
public class TestReadWrite extends TestCase {
	private InputStream getTestFile() throws IOException {
		return this.getClass().getResourceAsStream("/testVORBIS.ogg");
	}
	
	/**
	 * WARNING - assumes only one stream!
	 */
	private static long[] copy(OggFile in, OggFile out) throws IOException {
		OggPacketReader r = in.getPacketReader();
		
		ArrayList<Long> copyCRCs = new ArrayList<Long>();
		
		OggPacket p = null;
		OggPacket pp = null;
		OggPacketWriter w = null;
		while( (p = r.getNextPacket()) != null ) {
			if(w == null) {
				w = out.getPacketWriter(p.getSid());
				copyCRCs.add(p._getParent().getChecksum());
			}
			
			if(pp != null && pp.getSequenceNumber() != p.getSequenceNumber()) {
				w.flush();
				copyCRCs.add(p._getParent().getChecksum());
			}
			
			long oldGranule = p.getGranulePosition();
			w.bufferPacket(p);
			w.setGranulePosition(oldGranule);
			
			pp = p;
		}
		
		w.close();
		out.close();
		
		// Grab the checksums
		long[] ret = new long[copyCRCs.size()];
		for(int i=0; i<ret.length; i++) {
			ret[i] = copyCRCs.get(i);
		}
		return ret;
	}
	
	public void testReadWrite() throws IOException {
		OggFile in = new OggFile(getTestFile());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OggFile out = new OggFile(baos);
		
		copy(in, out);
		out.close();
	}
	
	public void testReadWriteContents() throws IOException {
		// Load the file and write it back out again
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OggFile out = new OggFile(baos);

		copy(new OggFile(getTestFile()), out);
		
		// Read the same file again to compare
		InputStream inp = getTestFile();
		byte[] original = new byte[4241];
		IOUtils.readFully(inp, original);
		assertEquals(-1, inp.read());
		
		// Check the raw bytes match
		byte[] readwrite = baos.toByteArray();
		
		assertEquals(original.length, readwrite.length);
		for(int i=0; i<original.length; i++) {
			assertEquals(original[i], readwrite[i]);
		}
	}
	
	public void testReadWriteRead() throws IOException {
		// Load the file and write it back out again
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OggFile out = new OggFile(baos);

		long[] crcs = copy(new OggFile(getTestFile()), out);
		
		// Load it in
		OggFile in = new OggFile(new ByteArrayInputStream(baos.toByteArray()));
		OggPacketReader r = in.getPacketReader();
		
		// Check roughly at a page level
		OggPacket p;
		while( (p = r.getNextPacket()) != null ) {
			int page = p._getParent().getSequenceNumber();
			assertEquals(crcs[page], p._getParent().getChecksum());
		}
	}
	
	public void testPackets() throws IOException {
		// Copy
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OggFile out = new OggFile(baos);

		copy(new OggFile(getTestFile()), out);
		
		// Now check
		OggFile ogg = new OggFile(
				new ByteArrayInputStream(baos.toByteArray())
		);
		OggPacketReader r = ogg.getPacketReader();
		OggPacket p;

		// Has 3 pages
		//  1 packet
		//  2 packets (2nd big)
		//  9 packets
		
		// Page 1
		p = r.getNextPacket();
		assertEquals(0x1e, p.getData().length);
		assertEquals(true, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0, p.getGranulePosition());
		assertEquals(0, p.getSequenceNumber());
		
		// Page 2
		p = r.getNextPacket();
		assertEquals(0xdb, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0, p.getGranulePosition());
		assertEquals(1, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(255*13+0xa9, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0, p.getGranulePosition());
		assertEquals(1, p.getSequenceNumber());
		
		// Page 3
		p = r.getNextPacket();
		assertEquals(0x23, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x1c, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x1e, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x34, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x33, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x45, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x3b, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x28, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(false, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(0x26, p.getData().length);
		assertEquals(false, p.isBeginningOfStream());
		assertEquals(true, p.isEndOfStream());
		assertEquals(0x0473b45c, p.getSid());
		assertEquals(0x3c0, p.getGranulePosition());
		assertEquals(2, p.getSequenceNumber());
		
		p = r.getNextPacket();
		assertEquals(null, p);
	}
}
