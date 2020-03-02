import numpy as np
from smac.configspace import ConfigurationSpace
from ConfigSpace.hyperparameters import UniformFloatHyperparameter

from smac.tae.execute_func import ExecuteTAFuncDict
from smac.scenario.scenario import Scenario
from smac.facade.smac_facade import SMAC

cs = ConfigurationSpace()
cs.add_hyperparameter(UniformFloatHyperparameter("C", 0.001, 1000.0, default_value=1.0))
cs.add_hyperparameter(UniformFloatHyperparameter("D", 1.0, 100000.0, default_value=100.0))

def eval(cfg):
  print("Received configuration", cfg)
  return 0.0

scenario = Scenario({"run_obj": "quality",
			"runcount-limit": 200,
			"paramfile": "./out.pcs",
			"deterministic": "true"
			})

print("create smac object")
smac = SMAC(scenario=scenario, rng=np.random.RandomState(42), tae_runner=eval)
print("Start optimization process")
smac.optimize()
print("Optimization done")