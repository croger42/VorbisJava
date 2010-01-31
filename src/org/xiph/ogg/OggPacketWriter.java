package org.xiph.ogg;

import java.io.IOException;
import java.util.ArrayList;

public class OggPacketWriter {
	private boolean closed = false;
	private OggFile file;
	private int sid;
	
	private static final int MAX_PAGE_DATA_SIZE = 65275;
	
	private ArrayList<OggPacket> buffer =
		new ArrayList<OggPacket>();
	
	protected OggPacketWriter(OggFile parentFile, int sid) {
		this.file = parentFile;
		this.sid = sid;
	}
	
	/**
	 * Buffers the given packet up ready for
	 *  writing to the stream, but doesn't
	 *  write it to disk yet.
	 */
	public void bufferPacket(OggPacket packet) {
		if(closed) {
			throw new IllegalStateException("Can't buffer packets on a closed stream!");
		}
		packet.setSid(sid);
		buffer.add(packet);
		
		// TODO
	}
	
	/**
	 * Buffers the given packet up ready for
	 *  writing to the file, and then writes
	 *  it to the stream if indicated.
	 */
	public void bufferPacket(OggPacket packet, boolean flush) throws IOException {
		bufferPacket(packet);
		if(flush) {
			flush();
		}
	}
	
	/**
	 * Returns the number of bytes (excluding headers)
	 *  currently waiting to be written to disk.
	 * RFC 3533 suggests that pages should normally 
	 *  be in the 4-8kb range.
	 * If this size exceeds just shy of 64kb, then
	 *  multiple pages will be needed in the underlying
	 *  stream.
	 */
	public int getSizePendingFlush() {
		int size = 0;
		for(OggPacket p : buffer) {
			size += p.getData().length;
		}
		return size;
	}
	
	/**
	 * Writes all pending packets to the stream,
	 *  splitting across pages as needed.
	 */
	public void flush() throws IOException {
		if(closed) {
			throw new IllegalStateException("Can't flush packets on a closed stream!");
		}
		
		int numPages = (int)Math.ceil(
				((double)getSizePendingFlush()) /  MAX_PAGE_DATA_SIZE
		);
		OggPage[] pages = new OggPage[numPages]; 
		
		// TODO Make the packets into pages
		
		// Write in one go
		file.writePages(pages);
			
		// Get ready for next time!
		buffer.clear();
	}
	
	/**
	 * Writes all pending packets to the stream,
	 *  with the last one containing the End Of Stream
	 *  Flag, and then closes down.
	 */
	public void close() throws IOException {
		if(buffer.size() > 0) {
			buffer.get( buffer.size()-1 ).setIsEOS();
		} else {
			OggPacket p = new OggPacket(new byte[0]);
			p.setIsEOS();
			buffer.add(p);
		}
		flush();
		
		closed = true;
	}
}
