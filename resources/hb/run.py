import logging
import json

logging.basicConfig(level=logging.WARNING)

import argparse
import os

import hpbandster.core.nameserver as hpns
import hpbandster.core.result as hpres

from hpbandster.optimizers import BOHB as BOHB
from hpbandster.optimizers import HyperBand as HyperBand
from evalworker import MyWorker

parser = argparse.ArgumentParser(description='Hyperband')
parser.add_argument('--min_budget', type=float, help='Minimum budget used during the optimization.', default=1)
parser.add_argument('--max_budget', type=float, help='Maximum budget used during the optimization.', default=5)
parser.add_argument('--eta', type=float, help='In each iteration, a complete run of sequential halving is executed. In it, after evaluating each configuraiton on the same subset size, only a fraction of 1/eta of them advances to the next round. Must be greater or equal to 2', default=2)
parser.add_argument('--id', type=int, help='ID of this run', default=0)
parser.add_argument('--n_workers', type=int, help='Number of workers to run in parallel', default=2)
args = parser.parse_args()

# Step 1: Start a nameserver
# Every run needs a nameserver. It could be a 'static' server with a
# permanent address, but here it will be started for the local machine with the default port.
# The nameserver manages the concurrent running workers across all possible threads or clusternodes.
# Note the run_id argument. This uniquely identifies a run of any HpBandSter optimizer.
runID='hb'+str(args.id)
with open('client.conf') as json_file:
    config = json.load(json_file)
gRPC_port = str(config['gRPC_port']);

NS = hpns.NameServer(run_id=runID, host='127.0.0.1', port=None)
NS.start()

workers = []
for i in range(args.n_workers):
    w = MyWorker(gRPC_port=gRPC_port, nameserver='127.0.0.1', run_id=runID, id=i)
    w.run(background=True)
    workers.append(w)
    
with open("run.log", "a") as file:
    file.write("Eta " + str(args.eta)+"\n")
    
optimizer = HyperBand(configspace=MyWorker.get_configspace(),
    run_id=runID, nameserver='127.0.0.1', eta=args.eta,
    min_budget=args.min_budget, max_budget=args.max_budget)
res = optimizer.run(min_n_workers=args.n_workers)

# Step 4: Shutdown
# After the optimizer run, we must shutdown the master and the nameserver.
optimizer.shutdown(shutdown_workers=True)
NS.shutdown()

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
