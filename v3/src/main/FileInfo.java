package main;

import java.util.List;

public class FileInfo {
	List<String> file_path;
	long file_length;

    public FileInfo(long file_length, List<String> file_path) {
		this.file_length = file_length;
		this.file_path = file_path;
	}
}
