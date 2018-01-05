import unittest
import requests
import json
import re
import grep_client

HOSTS = [
    "http://fa17-cs425-g51-%s.cs.illinois.edu" % str(n + 1).zfill(2)
    for n in range(10)
    ]
PORT = 5000

def _print(s):
    if True:
        print(s)

class ServerTestCase(unittest.TestCase):

    def test_log_wrong_parameter(self):
        _print("Testing test_log_wrong_parameter")
        for hostname in HOSTS:
            r = requests.get("%s:%d/generate_log?wrongParameter=100" % (hostname, PORT))
            self.assertEqual(r.status_code, 400)
    
    def test_log_wrong_parameter_value(self):
        _print("Testing test_log_wrong_parameter_value")
        for hostname in HOSTS:
            r = requests.get("%s:%d/generate_log?staticLog=notNumber&randomLog=100" % (hostname, PORT))
            self.assertEqual(r.status_code, 400)
            
            r = requests.get("%s:%d/generate_log?staticLog=-1&randomLog=100" % (hostname, PORT))
            self.assertEqual(r.status_code, 400)

    def test_static_log(self):
        _print("Testing test_static_log")
        for hostname in HOSTS:
            r = requests.get("%s:%d/generate_log?staticLog=10&randomLog=100" % (hostname, PORT))
            self.assertEqual(r.status_code, 200)
            log = r.text.split('\n')[:-1]
            self.assertEqual(r.text.count('\n'), 10 + 100)
            self.assertTrue('log-generator [DEBUG] http://www.imsdb.com/scripts/Star-Wars-A-New-Hope.html' in log[0])
            self.assertTrue('log-generator [DEBUG] STAR WARS' in log[2])
            self.assertTrue('log-generator [DEBUG] Episode IV' in log[4])
            self.assertTrue('log-generator [DEBUG] A NEW HOPE' in log[6])

            r = requests.get("%s:%d/generate_log?staticLog=123&randomLog=100" % (hostname, PORT))
            self.assertEqual(r.status_code, 200)
            log = r.text.split('\n')[:-1]
            self.assertEqual(r.text.count('\n'), 123 + 100)
            self.assertTrue('log-generator [DEBUG] http://www.imsdb.com/scripts/Star-Wars-A-New-Hope.html' in log[0])
            self.assertTrue('log-generator [DEBUG] STAR WARS' in log[2])
            self.assertTrue('log-generator [DEBUG] Episode IV' in log[4])
            self.assertTrue('log-generator [DEBUG] A NEW HOPE' in log[6])
            self.assertTrue('log-generator [DEBUG] A long time ago, in a galaxy far, far, away...' in log[21])   

    def test_wrong_grep(self):
        _print("Testing test_wrong_grep")
        for hostname in HOSTS:
            r = requests.post("%s:%d/grep" % (hostname, PORT), "grep --wrong arguments")
            result = json.loads(r.text)
            self.assertEqual(r.status_code, 200)
            self.assertEqual(result['status'], 2)
            self.assertTrue('grep: unrecognized option' in result['result'])
            self.assertTrue("Try 'grep --help' for more information" in result['result'])

    def test_empty_result_grep(self):
        _print("Testing test_empty_result_grep")
        for hostname in HOSTS:
            r = requests.post("%s:%d/grep" % (hostname, PORT), "grep no_such_string machine.log")
            result = json.loads(r.text)
            self.assertEqual(result['status'], 1)

    def test_simple_grep(self):
        _print("Testing test_simple_grep")
        for hostname in HOSTS:
            r = requests.post("%s:%d/grep" % (hostname, PORT), "grep her machine.log")
            result = json.loads(r.text)
            logs = result['result'].split('\n')[:-1]
            self.assertEqual(r.status_code, 200)
            self.assertEqual(result['status'], 0)
            for line in logs:
                self.assertTrue("her" in line)

    def test_regex_grep(self):
        _print("Testing test_regex_grep")        
        for hostname in HOSTS:
            regex = '^2017.*log-generator.*is.*'
            r = requests.post("%s:%d/grep" % (hostname, PORT), "grep -E %s machine.log" % regex)
            result = json.loads(r.text)
            logs = result['result'].split('\n')[:-1]
            # Static log will match 15 lines
            self.assertTrue(len(logs) >= 15 and len(logs) <= 120)
            for line in logs:
                self.assertTrue(re.search(regex, line) != None) # Line matched the regex

# Assume the user inputs a grep command only.
class ClientTestCase(unittest.TestCase):

    # def test_get_host_number(self):
    #     _print("Testing test_get_host_number")        
    #     self.assertEqual(3, grep_client.get_host_number("http://fa17-cs425-g51-03.cs.illinois.edu"))
    #     self.assertEqual(10, grep_client.get_host_number("http://fa17-cs425-g51-10.cs.illinois.edu"))
    #     self.assertEqual(0, grep_client.get_host_number("http://fa17-cs425-g51-00.cs.illinois.edu"))
    #     self.assertEqual(65, grep_client.get_host_number("http://fa17-cs425-g51-66.cs.illinois.edu"))
    #     self.assertEqual(0, grep_client.get_host_number("http://fa17-cs425-g51-100.cs.illinois.edu"))
    #     self.assertEqual(0, grep_client.get_host_number("http://fa17-cs425-g51-ab.cs.illinois.edu"))
    #     self.assertEqual(0, grep_client.get_host_number("hfa17-cs425-g51-66.cs.illinois.edu"))

    def test_client_wrong_input(self):
        _print("Testing test_client_wrong_input")        
        result = grep_client.grep(HOSTS[2], PORT, "grep", "grep --wrong parameters")
        _print(result)
        self.assertTrue('grep: unrecognized option' in result)
        self.assertTrue("Try 'grep --help' for more information" in result)

    def test_client_simple_grep(self):
        _print("Testing test_client_simple_grep")        
        result = grep_client.grep(HOSTS[3], PORT, "grep", "grep STAR machine.log")
        _print(result)
        self.assertTrue('STAR WARS' in result)

    def test_client_regex_grep(self):
        _print("Testing test_client_regex_grep")   
        regex = '^2017.*log-generator.*is.*'     
        result = grep_client.grep(HOSTS[7], PORT, "grep", "grep %s machine.log" % regex)
        _print(result)
        logs = result.split('\n')[:-1]
        for line in logs:
            self.assertTrue(re.search(regex, line) != None)

if __name__ == '__main__':
    unittest.main()