import requests # http://www.python-requests.org/en/latest/
import sys
import re
import json
import time
from threading import Thread

# The hostnames and ips of the 10 nodes
HOSTS = [
    "http://fa17-cs425-g51-%s.cs.illinois.edu" % str(n + 1).zfill(2)
    for n in range(10)
    ]
# The port which Flask server is runnning on
PORT = 5000

def print_message(list):
    print('\n'.join(list))

def get_host_number(hostname):
    m = re.search('http://fa17-cs425-g51-(\d\d).cs.illinois.edu', hostname)
    if m:
        return int(m.group(1))
    else:
        return 0

def grep(host, port, path, command):
    '''Post an HTTP request to server to execute user's command in shell.
        
    In MP1, the command should be a grep command.
    Shell Injection is not check. (Allowed by Prof. Indy)

    Input:
        The command to execute, which is carried by HTTP body as plain text.

    Output:
        A JSON object indicating status and result of the command.
        Grep return code reference: https://www.gnu.org/software/grep/manual/html_node/Exit-Status.html
        Status 0 means successful.
        Status 1 means no lines were selected.
        Status 2 means an error occurred in grep.
        Status 3 means the command returned a unexpected non-zero return code.
        Status 4 means execution timeout.
        Status 5 means unexpected error.
        The result is the stdout and stderr of the command, except for unexpected error which returns empty string.
    '''
    output = []
    url = "%s:%d/%s" % (host, port, path)
    try:
        data = json.loads(requests.post(url, command).text)
        output.append("==== Result from %s ====" % url)
        status = data['status']
        if status == 0:
            output.append('The command was executed successfully.\n')
        elif status == 1:
            output.append('No lines were selected.\n')
        elif status == 2:
            output.append('An error occurred in grep.\n')
        elif status == 3:
            output.append('The command returned a unexpected non-zero return code.\n')
        elif status == 4:
            output.append('Timeout for the command.')
        elif status == 5:
            output.append('Unexpected error on server side.')
        else:
            raise json.JSONDecodeError
        result = data['result']
        output.append("Total lines: " + str(len(result.split('\n'))))
        output.append("----  End ----\n")
        print_message(output)
        return result
    except json.JSONDecodeError:
        print("Invalid JSON object.")
    except:
        print("XXXX Connection to %s failed. XXXX" % host)

if __name__ == '__main__':
    while True:
        sys.stdout.write('Command: ')
        sys.stdout.flush()

        command = sys.stdin.readline().strip()
        threads = []

        start_time = time.time()

        # Run user's command parallelly in different threads.
        for host in HOSTS:
            thread = Thread(target = grep, args = (host, PORT, 'grep', command))
            thread.start()
            threads.append(thread)
        
        # Wait for all the threads to finish
        for thread in threads:
            thread.join()

        end_time = time.time()
        print("Total time", round(end_time - start_time, 4))