import pm4py
from pm4py.objects.log.importer.xes import importer as xes_importer
import sys

def logstats(logfile):
    print (logfile)
    log = xes_importer.apply(logfile)
    events = 0
    for trace in log:
        events += len(trace)
    print("Log: {} Traces: {} Events: {} ".format( logfile, len(log), events ) )


if __name__ == "__main__":
    logstats(sys.argv[1])

