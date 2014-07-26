import storm

import time
import sys
import threading
import base64
import json
import numpy as np
import threading
from threading  import Thread
import Queue
import ctypes
from subprocess import PIPE, Popen
from StringIO import StringIO

from modules import Planning
from modules import Tracking

lock = threading.RLock()

# this is the class we interface with storm. This will process the incoming messages by decoding them,
# do the image processing and create a command and emit it.
class PlanningBolt(storm.Bolt):
    def __init__(self):
        self.planing = Planning.Planning()

    # this method will be called by storm when there is an incoming frame
    def process(self, tup):
        targets = tup.get(0)
        original_time = tup.get(1)
        command = self.planing.do(None, targets)

        message = {"command": [command.tiltx, command.tilty, command.spin, command.velocity_z]}
        io = StringIO()
        json.dump(message, io)

        storm.emit([io.getvalue(), original_time])

PlanningBolt().run()