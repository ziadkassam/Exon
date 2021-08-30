Compile:  
--------
javac -sourcepath src -classpath classes: -d target/classes src/main/java/haslab/eo/*.java src/main/java/haslab/eo/events/*.java src/main/java/haslab/eo/msgs/*.java src/main/java/io/github/pssalmeida/slidingbitmap/*.java src/test/java/haslab/eo/*.java

//Oneway
--------
Receiver> java -classpath target/classes haslab.eo.Receiver host port noMsgs msgSize comType N
Sender>   java -classpath target/classes haslab.eo.Sender host port noMsgs msgSize comType N

//RPC
-----
Server> java -classpath target/classes haslab.eo.Server host port N
Client> java -classpath target/classes haslab.eo.Client host port N noMsgs msgSize actors

---------------------------------------
- comType: 1:unidirectional 2:bidirectional
- N: number of slots to be kept in reserve
- actors: number of simulated actors (virtual clients)
