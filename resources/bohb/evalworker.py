from hpbandster.core.worker import Worker
from ConfigSpace.read_and_write import pcs_new
import grpc
import json
import math
import os, sys, inspect
import PCSBasedComponentParameter_pb2
import PCSBasedComponentParameter_pb2_grpc


class MyWorker(Worker):
    component = ''

    def __init__(self, *args, sleep_interval=0, gRPC_port, **kwargs):
        super().__init__(*args, **kwargs)
        self.gRPC_port = gRPC_port
        self.log = open("worker.log", "a")
        self.name = "w"

    def compute(self, config, budget, **kwargs):
        """
        Simple example for a compute function
        The loss is just a the config + some noise (that decreases with the budget)

        For dramatization, the function can sleep for a given interval to emphasizes
        the speed ups achievable with parallel workers.

        Args:
            config: dictionary containing the sampled configurations by the optimizer
            budget: (float) amount of time/epochs/etc. the model can use to train

        Returns:
            dictionary with mandatory fields:
                'loss' (scalar)
                'info' (dict)
        """

        self.log.write(self.name + ": Evaluate configuration " + str(config) + " with budget " + str(budget) +"\n")
        params = []
        for k, v in config.items():
            params.append(PCSBasedComponentParameter_pb2.PCSBasedParameterProto(key=k, value=str(v)))
        cmp = PCSBasedComponentParameter_pb2.PCSBasedComponentProto(name=str(math.floor(budget)), parameters=params)
        self.log.write(self.name + ": Built protobuf component " + str(cmp) +"\n")

        self.log.write(self.name + ": Connect to evaluation backend\n")
        channel = grpc.insecure_channel("localhost:" + str(self.gRPC_port))
        stub = PCSBasedComponentParameter_pb2_grpc.PCSBasedOptimizerServiceStub(channel)


        self.log.write(self.name + ": Evaluate component \n")
        response = stub.Evaluate(cmp)
        channel.close()
        self.log.write(self.name + ": Received response " + str(response) + " and closed the connection.\n")

        # res = numpy.clip(config['x'] + numpy.random.randn() / budget, config['x'] / 2, 1.5 * config['x'])
        # config['weka.classifiers.bayes.BayesNet.Q']
        # time.sleep(self.sleep_interval)
        self.log.write(self.name + ": Result " + str(response.result)+"\n")

        if(response.result < 0):
            return( {'loss': float(10000), 'info': 'crashed'})
        else:
            return ({'loss': float(response.result), 'info': 'succeeded'})

    @staticmethod
    def get_configspace():
        with open('searchspace.pcs', 'r') as fh:
            cs = pcs_new.read(fh)
            return cs