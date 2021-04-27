foo:
	javac src/bencoding/*.java -d ./
	javac -cp ./ src/main/*.java -d ./
	jar cvfm client.jar manifest.txt bencoding/*.class main/*.class

clean:
	rm -rf bencoding
	rm -rf main
	rm client.jar