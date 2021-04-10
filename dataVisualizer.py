# import queue
# import subprocess
# import threading


# class AsynchronousFileReader(threading.Thread):
#     '''
#     Helper class to implement asynchronous reading of a file
#     in a separate thread. Pushes read lines on a queue to
#     be consumed in another thread.
#     '''

#     def __init__(self, fd, queue):
#         assert isinstance(queue, queue.Queue)
#         assert callable(fd.readline)
#         threading.Thread.__init__(self)
#         self._fd = fd
#         self._queue = queue

#     def run(self):
#         '''The body of the tread: read lines and put them on the queue.'''
#         for line in iter(self._fd.readline, ''):
#             self._queue.put(line)

#     def eof(self):
#         '''Check whether there is no more content to expect.'''
#         return not self.is_alive() and self._queue.empty()


# # You'll need to add any command line arguments here.
# process = subprocess.Popen(["adb", "logcat" ], stdout=subprocess.PIPE)

# # Launch the asynchronous readers of the process' stdout.
# stdout_queue = queue.Queue()
# stdout_reader = AsynchronousFileReader(process.stdout, stdout_queue)
# stdout_reader.start()

# # Check the queues if we received some output (until there is nothing more to get).
# while not stdout_reader.eof():
#     while not stdout_queue.empty():
#         line = stdout_queue.get()
#         print(line)

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import sys

tags = ["myGravityComplementary"]
columns = ["time", "x", "y", "z", "w"]
axis = ["x", "y", "z"]
data = pd.DataFrame(columns=columns)
max_row = 20
for line in sys.stdin:
    log = line.rstrip()
    for tag in tags:
        pos = log.find(tag)
        if pos != -1 :
            logsplit = np.array(log[pos+len(tag)+3:].split(", "), dtype='float64')
            data.loc[len(data)] = logsplit
            data = data.tail(max_row)
            plt.cla()
            for c in axis:
                plt.plot(data["time"], data[c], label=c)
            plt.title(tag)
            plt.legend()
      