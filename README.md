A PoC library implementation of Exon: Exactly-Once Oblivious Messaging Protocol
---

Exon is a host-to-host message-based protocol that is op- timized to guarantee the exactly-once (EO) delivery of these messages. This is possible through the concept of reserving slots at the destination host before sending any payload. When a slot is first consumed at the destination host, it is deleted and, therefore, duplication will not occur no mat- ter how many retransmissions are done, e.g., given possible network issues.


Features Highlights
---


Exon has a combination of ingredients which allows ensuring exactly-once delivery over unreliable networks, while being network and memory efficient, namely:

- **Message-based**: conceptually, Exon is a four-way protocol per message (not byte segments); thus treating each message separately, assigning each message a unique identifier.
- **No connection API**: Exon does not have a connection management API (no connect or close) nor explicit connection management protocol messages.
- **Soft-connections**: connections are useful to group identifiers like sequence numbers and achieve performance. We have what we call soft-connections, that group messages from the same sender-receiver pair, created on-demand if messages are requested to be sent, and discarded if there are no pending unacknowledged messages, after some inactivity.
- **Half-connection based**: our soft-connections are half-duplex, managing each direction independently by an individual soft-connection. 
- **No time-dependence for correctness**: Exon ensures exactly-once correctness without depending on any timeouts (such as for the TCP (TIME_WAIT state), using only timeouts for performance related decisions, such as deciding when to retransmit.
- **Oblivious**: Exon achieves correctness without the need to keep connection-related information forever, keeping only a single integer per node as permanent state, when no soft-connections are present.
- **Order-less**: to be more generic, Exon is deprived from unnecessary ordering restrictions of messages. Message ordering (e.g., FIFO) can easily be implemented on top of \pro if required.


Getting started
---


**Compile**
javac -sourcepath src -classpath classes: -d target/classes src/main/java/haslab/eo/*.java src/main/java/haslab/eo/events/*.java src/main/java/haslab/eo/msgs/*.java src/main/java/io/github/pssalmeida/slidingbitmap/*.java src/test/java/haslab/eo/*.java

Run
--
- comType: 1:unidirectional 2:bidirectional
- N: number of slots to be kept in reserve
- actors: number of simulated actors (virtual clients)
-----------------------------------------

**Oneway**
Receiver> java -classpath target/classes haslab.eo.Receiver host port noMsgs msgSize comType N

Sender>   java -classpath target/classes haslab.eo.Sender host port noMsgs msgSize comType N

**RPC**
Server> java -classpath target/classes haslab.eo.Server host port N

Client> java -classpath target/classes haslab.eo.Client host port N noMsgs msgSize actors
