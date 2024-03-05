# Peer to Peer

## Members: Dimitris Tzovanis, Elpida Stasinou


This project implements a basic peer-to-peer (P2P) file sharing system using Java. 
It includes components for peers, a tracker for managing peers and files within the network, 
and supporting classes for peer information and communication threads.

### Components
- Peer: Implements the functionality of a peer within the P2P network, handling file sharing, downloading, and communication with other peers and the tracker.
- PeerInfo: Stores information about peers, such as IP address, port, and shared files.
- PeerServer: Manages incoming connections from other peers and facilitates file transfers.
- PeerThread: Handles individual peer-to-peer communication sessions.
- Tracker: Acts as the central server that tracks online peers and the files they share. It also manages peer registration and lookup.
- TrackerThread: Handles communication between the tracker and peers, processing requests such as file registration and peer discovery.


### Features
- Dynamic peer discovery and registration.
- File sharing and downloading among peers.
- Centralized tracking of peer availability and shared files.
- Checks which peers are active and their response time


### Usage
- Start the Tracker to initiate the tracking server.
- Launch Peer instances to join the network.
- Register and Login as peer
- Search files
- Check which peers have such files and are active, then proceed to download
- Logout
