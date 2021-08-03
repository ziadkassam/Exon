Compile:  
--------
javac -sourcepath src -classpath classes: -d target/classes src/main/java/haslab/eo/*.java src/main/java/haslab/eo/events/*.java src/main/java/haslab/eo/msgs/*.java src/main/java/io/github/pssalmeida/slidingbitmap/*.java src/test/java/haslab/eo/*.java


//comType=1: one-way  --  comType=2: bidirectional not RPC
------------------------------------------
Receiver> java -classpath target/classes haslab.eo.Receiver host port noMsgs msgSize comType N
localhost: java -classpath target/classes haslab.eo.Receiver localhost 1234 10000 1024 1 220
Emulab: java -classpath target/classes haslab.eo.Receiver client 1234 10000 1024 1 220

Sender> java -classpath target/classes haslab.eo.Sender host port noMsgs msgSize comType N
localhost: java -classpath target/classes haslab.eo.Sender localhost 3456 10000 1024 1 220
Emulab: java -classpath target/classes haslab.eo.Sender server 3456 10000 1024 1 220


//Request-Response
---------------------------------------
Server> java -classpath target/classes haslab.eo.Server host port N
localhost:java -classpath target/classes haslab.eo.Server localhost 1234 1
Emulab: java -classpath target/classes haslab.eo.Server client 1234 50

Client> java -classpath target/classes haslab.eo.Client host port N noMsgs msgSize actors
localhost:java -classpath target/classes haslab.eo.Client localhost 3456 1 10000 1024 1
Emulab: java -classpath target/classes haslab.eo.Client server 3456 50 1000 1024 1



---------------------------------------
- h: the host to communicate with
- p: Port or the receiver
- n: number of messages
- s: message size
- N: number of slots to be kept in reserve
- comType: communication type, 1=one-way, 2=send/receive.
