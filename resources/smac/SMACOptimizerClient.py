import json
import grpc
import os
import sys
import inspect
import PCSBasedComponentParameter_pb2
import PCSBasedComponentParameter_pb2_grpc

if __name__ == '__main__':
    # create logger file object
    log = open("client.log", "a")
    log.write("Client received call with arguments " + str(sys.argv))
    log.write("Read client.conf\n")
    # read client conf
    with open('client.conf') as json_file:
        config = json.load(json_file)
    
    gRPC_port = str(config['gRPC_port']);
    log.write("Run client with port: " + gRPC_port+"\n")
    params = []

    log.write("Build protobuf message\n")
    for i in range(6, len(sys.argv) - 1, 2):
        params.append(PCSBasedComponentParameter_pb2.PCSBasedParameterProto(key=sys.argv[i][1:].replace("-", ""),value=sys.argv[i + 1]))
        
    cmp = PCSBasedComponentParameter_pb2.PCSBasedComponentProto(name="5", parameters=params)

    log.write("Open connection to grpc server via 127.0.0.1:" + gRPC_port + "\n")
    channel = grpc.insecure_channel("127.0.0.1:"+gRPC_port)
    stub = PCSBasedComponentParameter_pb2_grpc.PCSBasedOptimizerServiceStub(channel)
    
    log.write("Evaluate candidate\n")
    response = stub.Evaluate(cmp)
  
    channel.close()
    log.write("Closed connection to grpc server")
    log.close()
    
    print("\n")
    if(response.result >= 0):
        print("Result for SMAC: SUCCESS,-1,-1,"+str(response.result)+",5")
    elif (response.result == -1):
        print("Result for SMAC: TIMEOUT,-1,-1,-1,5")
    elif (response.result == -2):
        print("Result for SMAC: MEMOUT,-1,-1,-1,5")
    else:
        print("Result for SMAC: CRASHED,-1, -1,-1,5")
        
        