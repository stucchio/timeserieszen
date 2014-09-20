#!/usr/bin/env python
"""
This is just a script used for testing.
"""

import socket
import time
import random
import sys


CARBON_SERVER = '0.0.0.0'
CARBON_PORT = 2003

series = ['neha.sharma', 'zoe.saldana', 'foxy.cleopatra', 'server.load', 'server.ram']

def datapoint():
    return " ".join([random.choice(series), str(random.random()*100), str(int(time.time()*1000))])

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
for i in range(int(sys.argv[1])):
    sock.sendto(bytes(datapoint()), (CARBON_SERVER, CARBON_PORT))
sock.close()
