# Distributed Chat [![GPLv3 license](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://github.com/CodePeters/Pacman/blob/master/LICENSE)

** A distributed chat implemented in Java. There are two main classes-componentes. Firstly the Tracker (Tracker.java) which keeps track of all users by registering new users, keeping informatiion about them (e.g Udp ports) and  also checking if someone goes down where in this case deletes this user. The other basic component is the client (client). You can start a new client in order to participate in one or more team chats. When designing a distributed chat system the main topic is whether each user will see the group messages in the same order that other users see them, because messages may arrive in different order in different client. To solve this issue we implemented two modes, the first one guarantees fifo ordering and the second mode uses isis algorithm for total ordering. Also since this is a chat system, each client needs to run multiple threads for example one thread to listen to a Udp port for new messages and another thread for sending new messages, so we need to use some locks for handling concurrent access and writes to data structures. In most cases we used concurrent data structures that java library provides for high performance. **
______________________________________

## Contributors:

* Goerge Petrou
* Nick Dragazis
* Panagiotis Gouskos


## Compililation:

Compilation is done used Makefile. The Makefile is located in the directory: `Project/src/Distrib`
Inside this directory run:

```
Make
```

and the class generated code will be created in the `Project/out/production/Project/Distrib` directory.


## Execution:

To start the Tracker:

```
java Distrib.Tracker
```

And to start a client for participating in a chat:

```
java Distrib.Client Port OrderingOption
```
* where Port is the Udp port number that the client listens for new messages.

* OrderingOption is either Fifo (for fifo ordering) or Total for Total+Fifo Ordering.


## List of Commands for Client:

* register: this command send information to the tracker and register user. Tracker keeps infi of all users on data structures.

* list_groups: with this command client communicates with the tracker and asks info about all the available groups.

* list_member(groupname): tracker returns the group members of group with name groupname.

* join_group(groupname): client sends tracker a request to join the group with name groupname. If such group does not exist tracker initializes an empty group woth this name and adds client.

* exit_group(groupname): clients send a request to tracker to delete him from this group.

* quit: client withdraws from messanger and tracker removes him from all of the group where participated.


## Test cases and execution examples:

Some testcases, simple .txt files have been added to the directory `Project/out/production/Project`. Below are some screenshot example of the chat system, firstly with fifo ordering:

<img src="fifo.png">

and total ordering:

<img src="total.png">


## _License_

This project is licensed under the GPLv3 License - see the [LICENSE](LICENSE) file for details