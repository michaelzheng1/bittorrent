package main;

public class BlockInfo {
	int idx;
	int offset;
	int size;
	
	public BlockInfo(int idx, int offset, int size) {
		this.idx = idx;
		this.offset = offset;
		this.size = size;
	}
	
	public String toString() {
		return String.format("idx:%d offset:%d size:%d", idx, offset, size);
	}
}
