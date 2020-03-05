import logging
import json

logging.basicConfig(level=logging.WARNING)

import argparse
import os

import hpbandster.core.nameserver as hpns
import hpbandster.core.result as hpres

from hpbandster.optimizers import BOHB as BOHB
from evalworker import MyWorker

log = open("run.log", "a")
log.write("Parse arguments\n")
log.flush()
parser = argparse.ArgumentParser(description='BOHB')
parser.add_argument('--min_budget', type=float, help='Minimum budget used during the optimization.', default=1)
parser.add_argument('--max_budget', type=float, help='Maximum budget used during the optimization.', default=5)
parser.add_argument('--n_iterations', type=int, help='Number of iterations performed by the optimizer', default=4)
parser.add_argument('--id', type=int, help='ID of this run', default=0)
parser.add_argument('--n_workers', type=int, help='Number of workers to run in parallel', default=2)
args = parser.parse_args()

log.write("Start runner\n")
log.flush()
# Step 1: Start a nameserver
# Every run needs a nameserver. It could be a 'static' server with a
# permanent address, but here it will be started for the local machine with the default port.
# The nameserver manages the concurrent running workers across all possible threads or clusternodes.
# Note the run_id argument. This uniquely identifies a run of any HpBandSter optimizer.
runID='hb'+str(args.id)
log.write("Run ID is " + runID + ". Now create nameserver\n")
log.flush()
NS = hpns.NameServer(run_id=runID, host='127.0.0.1', port=None)
NS.start()
log.write("Created nameserver with runID " + runID +"\n")
log.flush()

with open('client.conf') as json_file:
    config = json.load(json_file)
gRPC_port = str(config['gRPC_port']);
    
workers = []
for i in range(args.n_workers):
    w = MyWorker(gRPC_port=gRPC_port, nameserver='127.0.0.1', run_id=runID, id=i)
    w.run(background=True)
    workers.append(w)
    
optimizer = BOHB(configspace=MyWorker.get_configspace(),
    run_id=runID, nameserver='127.0.0.1',
    min_budget=args.min_budget, max_budget=args.max_budget)
log.write("created optimizer, now let it run\n")
log.flush()
res = optimizer.run(n_iterations=args.n_iterations, min_n_workers=args.n_workers)

# Step 4: Shutdown
# After the optimizer run, we must shutdown the master and the nameserver.
log.write("shutdown optimizer\n")
log.flush()
optimizer.shutdown(shutdown_workers=True)
log.write("optimizer shut down\n")
log.flush()
NS.shutdown()
log.write("Shut down nameserver\n")
log.flush()

# Step 5: Analysis
# Each optimizer returns a hpbandster.core.result.Result object.
# It holds informations about the optimization run like the incumbent (=best) configuration.
# For further details about the Result object, see its documentation.
# Here we simply print out the best config and some statistics about the performed runs.
id2config = res.get_id2config_mapping()
incumbent = res.get_incumbent_id()

print('Best found configuration:', id2config[incumbent]['config'])
print('A total of %i unique configurations where sampled.' % len(id2config.keys()))
print('A total of %i runs where executed.' % len(res.get_all_runs()))
print('Total budget corresponds to %.1f full function evaluations.' % (
            sum([r.budget for r in res.get_all_runs()]) / args.max_budget))
