import json
import os
import subprocess

from flask import Flask, request, abort

app = Flask(__name__)

@app.route("/grep", methods=['POST'])
def grep():
    '''Open a new process to run the received command and return the result.
    
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
    TIMEOUT = 20
    try:
        stdout = subprocess.check_output(request.data, shell=True, stderr=subprocess.STDOUT, timeout=TIMEOUT).decode('utf-8')
        return json.dumps({'status': 0, 'result': stdout})
    except subprocess.CalledProcessError as e:
        if e.returncode == 1 or e.returncode == 256:
            return json.dumps({'status': 1, 'result': e.stdout.decode('utf-8')})
        if e.returncode == 2 or e.returncode == 512:
            return json.dumps({'status': 2, 'result': e.stdout.decode('utf-8')})
        return json.dumps({'status': 3, 'result': e.stdout.decode('utf-8')})
    except subprocess.TimeoutExpired as e:
        return json.dumps({'status': 4, 'result': e.stdout.decode('utf-8')})
    except:
        return json.dumps({'status': 5, 'result': ''})

@app.route("/generate_log", methods=['GET'])
def generate_log():
    staticLog = request.args.get('staticLog', None)
    randomLog = request.args.get('randomLog', None)
    if not staticLog or not randomLog:
        abort(400)
    try:
        if int(staticLog) < 0 or int(randomLog) < 0:
            abort(400)
        os.system("python3.6 log_generator.py --staticLog %d --iterations %d" % (int(staticLog), int(randomLog)))
        log = subprocess.check_output("cat machine*log", shell=True, stderr=subprocess.STDOUT, timeout=10).decode('utf-8')
        return log
    except ValueError:
        abort(400)

if __name__ == "__main__":
    app.run(host='0.0.0.0')