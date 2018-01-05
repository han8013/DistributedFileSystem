# CS425 MP1: Distributed Log Querier

The program allows you to query distributed log files on multiple machines, from any one of those machines.

## Instructions

### Server side

The server is implemented by Flask and works over HTTP protocol. 

1. Install python3.6 and required libs on all hosts.
2. cd to the directory which contains `grep_server.py`
3. Run Flask server by executing `python3.6 grep_server.py`

### Client side

Client side sends HTTP requests to server and get response.

1. cd to the directory which contains `grep_client.py`
2. Run `python3.6 grep_client.py`
3. Input command like `grep STAR machine.log` and hit `return`
4. View the result from all servers

## Log file renaming

Files like `machine.[i].log` or `vm[i].log` are renamed to `machine.log` and `vm.log`

