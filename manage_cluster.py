import getopt
import sys
import os
import re
from threading import Thread

import paramiko # Document: http://docs.paramiko.org/en/2.2/

HOSTS = [
    ("fa17-cs425-g51-%s.cs.illinois.edu" % str(n + 1).zfill(2), 
    "172.22.154.1%s" % str(n + 2).zfill(2)) 
    for n in range(10)
    ]
FLASK_PORT = 5000
USERNAME = 'fl9'
COMMAND_CHECK = [
    ('cmd', -1),
    ('scp', 2),
    ('gitsync', 0),
    ('rm_log', 0),
    ('start_flask', 0),
    ('stop_flask', 0),
    ('restart_flask', 0),
    ('check_flask', 0)
]

USAGE_INFO = '''

Options:
    -p              (Default) Run commands parallelly on hosts.
    -s              Run commands sequentially(one-by-one) on hosts.
    -a              Run on all hosts.
    -h <num>        Run on selected host(s), <num> can specify multiple hosts, 1 for 01, 4 for 04, 0 for 10, etc.
                    e.g. -h 180 run on fa17-cs425-g51-[01|08|10].cs.illinois.edu
    -q              Quit

Commands:

    cmd             Run commands on all hosts.
                    Format: cmd <command>

    scp             Copy a file to all the hosts.
                    Format: scp <local file path> <remote file path>

    gitsync         Sync hosts with git repo

    start_flask     Start Flask server.
    stop_flask      Stop Flask server.
    restart_flask   Restart Flask server.
    check_flask     Check status of Flask server.

Note: Make sure your ssh key is authorized on the hosts.
'''

def get_host_number(hostname):
    m = re.search('http://fa17-cs425-g51-(\d\d).cs.illinois.edu', hostname)
    if m:
        return int(m.group(1))
    else:
        return 0

def scp(hostname, localfile, remotefile):
    """Transfer a file with scp

    Run scp command on local machine to transfer.

    Args:
        hostname (str): The hostname of the host.
        localfile (str): The path of the local file to be copied.
        remotefile (str): The path of the remote file to be pasted.
    """
    # ssh = paramiko.SSHClient()
    # ssh.load_system_host_keys()
    # ssh.connect(hostname, username=USERNAME)
    # sftp = ssh.open_sftp()
    # print(localfile, "  ", remotefile)
    # sftp.put(localfile, remotefile)
    # sftp.close()
    # ssh.close()
    if not os.path.exists(localfile):
        print("Path doesn't exist: %s" % localfile)
        exit(3)
    command = "scp"
    if os.path.isdir(localfile):
        command += " -r"
    os.system("%s %s %s@%s:%s" % (command, localfile, USERNAME, hostname, escape_user_path(remotefile)))

def run_cmd(hostname, cmd, timeout=10000.0):
    """Run command on a host

    Args:
        hostname (str): The hostname of the host.
        cmd (str): The command to run.

    Return:
        stdin, stdout and stderr of the command.
    """
    ssh = paramiko.SSHClient()
    ssh.load_system_host_keys()
    ssh.connect(hostname, username=USERNAME)
    print("Start to run '%s' on %s" % (cmd, hostname))
    ssh_stdin, ssh_stdout, ssh_stderr = ssh.exec_command(cmd, timeout=timeout)
    message = []
    message.append('==== Result from %s ====' % hostname)
    message.append('$ %s' % cmd)
    try:
        result = ssh_stdout.read().decode('utf-8')
        message.append('stdout:')
        message.append(result)
        result = ssh_stderr.read().decode('utf-8')
        message.append('stderr:')
        message.append(result)
        message.append("total lines:" + str(len(result.split('\n'))))
    except:
        message.append("Execution timeout. It's expected when starting the flask server.")
    message.append('---- End for %s ----' % hostname)
    message.append('\n')
    print("\n".join(message))
    ssh.close()
    return ssh_stdin, ssh_stdout, ssh_stderr

def rm_log(hostname):
    cmd = 'cd CS425/mp4/src && rm log.txt sdfs.txt && rm *.class && rm entity/*.class && rm graph/*.class'
    run_cmd(hostname, cmd)

def start_flask_server(hostname):
    cmd = 'cd CS425/mp1 && nohup python3.6 grep_server.py &'
    run_cmd(hostname, cmd)

def stop_flask_server(hostname):
    cmd = "kill -9 $(ps ax | grep grep_server | grep -v grep.*grep | cut -d' ' -f1)"
    cmd = "kill -9 $(ps ax | grep grep_server | grep -v grep.*grep | cut -d' ' -f2)"
    run_cmd(hostname, cmd)

def restart_flask_server(hostname):
    stop_flask_server(hostname)
    start_flask_server(hostname)

def check_flask_server(hostname):
    cmd = 'ps ax | grep grep_server | grep -v grep.*grep'
    run_cmd(hostname, cmd)

def print_usage():
    """Print script usage and exit the program.

    Mostly used when user input is wrong.
    """
    print(USAGE_INFO)

def escape_user_path(path):
    """Escape absolute path like '/Users/abc/a' to '~/a'

    getopt will automatically translate '~/a' to '/Users/abc/a', but we don't like so.
    This function is used to translate it back.
    """
    m = re.match(r'^/Users/[^/]*(/(.*)|$)', path)
    if m:
        return "~%s" % m.group(1)
    return path

def run_single_host(host, remainder):
    '''Single host operation

    Run a desired operation on a host.

    Args:
        host (str, str): Hostname and ip of the host
        remainder (list): List of the command and arguments

    '''
    hostname, ip = host
    cmd = remainder[0]
    args = remainder[1:]

    if cmd == 'cmd':
        args = [escape_user_path(x) for x in args]
        run_cmd(hostname, ' '.join(args))
    elif cmd == 'scp':
        scp(hostname, args[0], args[1])
    elif cmd == 'rm_log':
        rm_log(hostname)
    elif cmd == 'gitsync':
        run_cmd(hostname, 'cd CS425 && git pull origin master')
    elif cmd == 'start_flask':
        start_flask_server(hostname)
    elif cmd == 'stop_flask':
        stop_flask_server(hostname)
    elif cmd == 'restart_flask':
        restart_flask_server(hostname)
    elif cmd == 'check_flask':
        check_flask_server(hostname)

if __name__ == '__main__':
    
    # Print host information and script usage
    # print('Hosts:', HOSTS)
    print_usage()

    # Operate the hosts parallely or one-by-one.
    parallel = True

    # Record the index of the hosts to operate on
    selected_hosts = set([i for i in range(10)])

    # Main loop for reading and running commands
    while True:

        # Prompt message
        if len(selected_hosts) == 10:
            sys.stdout.write('All hosts')
        else:
            sys.stdout.write('Host %s' % ','.join([str(i + 1) for i in sorted(selected_hosts)]))
        if parallel:
            sys.stdout.write('(parallel): ')
        else:
            sys.stdout.write('(sequential):')
        sys.stdout.flush()

        # Read user input
        line = sys.stdin.readline().strip()

        # Analyze user input by getopt (Might changed later)
        try:
            options, remainder = getopt.getopt(line.split(' '), 'apqsh:')
        except:
            print_usage()
            continue
        # print("Options: ", options, remainder)

        # Change options
        for opt, val in options:
            if opt == '-p':
                parallel = True
            elif opt == '-s':
                parallel = False
            elif opt == '-h':
                new_hosts = set()
                for h in val.strip():
                    if h < '0' or h > '9':
                        print('Please specify correct hosts')
                        break
                    else:
                        new_hosts.add((int(h) - 1) % 10)
                else:
                    selected_hosts = new_hosts
            elif opt == '-a':
                selected_hosts = set([i for i in range(10)])
            elif opt == '-q':
                print("Bye")
                exit(0)

        if len(remainder) == 0:
            continue

        # Check if the command is valid
        for cmd, arg_size in COMMAND_CHECK:
            if cmd == remainder[0]:
                if arg_size == -1:
                    break
                if len(remainder) == arg_size + 1:
                    break
                else:
                    print("Wrong arguments for command '%s'." % cmd)
        else:
            print_usage()
            continue

        if parallel:
            # Run commands parallely on hosts, faster
            threads = []
            for host in [HOSTS[i] for i in sorted(selected_hosts)]:
                thread = Thread(target=run_single_host, args=(host, remainder))
                thread.start()
                threads.append(thread)
            for thread in threads:
                thread.join()
            
        else:
            # Run commands on-by-one on hosts, slower but clearer
            for host in [HOSTS[i] for i in sorted(selected_hosts)]:
                run_single_host(host, remainder)