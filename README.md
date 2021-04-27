## Bittorrent

An experimental BitTorrent client implemented in Java.

The client is able to interoperate with official BitTorrent client.

This is a group project from university.

Go to v3 for the current verison.


![Alt Text](https://github.com/michaelzheng1/website/blob/master/gif/bittorrent.gif?raw=true)

### Development Setup 

Recommend to download Oracle VM VirtualBox. 

```bash
Operating System: Ubuntu(64-bit)
```

To clone the project, open a terminal and run:

```bash
git clone https://github.com/michaelzheng1/bittorrent.git
```

Download using torrent file:

```bash
java -jar client.jar test_download_from_bittorrent.torrent
```

Start a server using 2333 port:

```bash
java -jar client.jar test_server_and_client.torrent server 2333 "BitTorrent.exe 
```

Start a client and establish TCP connection with server:
```bash
java -jar client.jar test_server_and_client.torrent client 2333 
```

### Tech Stack

- Java
- C
