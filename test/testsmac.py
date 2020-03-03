import numpy as np
import random
from smac.configspace import ConfigurationSpace
from ConfigSpace.hyperparameters import UniformFloatHyperparameter

from smac.tae.execute_func import ExecuteTAFuncDict
from smac.scenario.scenario import Scenario
from smac.facade.smac_facade import SMAC

scenario = Scenario({"run_obj": "quality",
			"runcount-limit": 200,
			"paramfile": "./searchspace.pcs",
			"deterministic": "true",
			"algo": "python SMACOptimizerClient.py",
			"wallclock-limit": 600
			})

print("create smac object")
smac = SMAC(scenario=scenario, rng=np.random.RandomState(42))
print("Start optimization process")
smac.optimize()
print("Optimization done")