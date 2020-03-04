import grpc
import os
import sys
import inspect
import PCSBasedComponentParameter_pb2
import PCSBasedComponentParameter_pb2_grpc

if __name__ == '__main__':
    myfile = open("algo-log.txt", 'a')
    myfile.write(str(sys.argv) + "\n")
    myfile.close()
    # find parameterfile pcs in scenario
    f = open("client.cfg", "r")
    log = open("client.log", "a")
    lines = f.readlines();
    for line in lines:
        if line.startswith("paramfile"):
            componentName = line.replace("paramfile = ", "")[:-5]
            log.write("Component name: " + str(componentName)+"\n")
        if line.startswith("gRPC_port"):
            gRPC_port = line.replace("gRPC_port = ", "").strip()
            log.write("Port: " + gRPC_port+"\n")

    params = []

    myfile = open("algo-log.txt", 'a')
    myfile.write("read in configs\n")
    myfile.close()
    
    for i in range(6, len(sys.argv) - 1, 2):
        param = PCSBasedComponentParameter_pb2.PCSBasedParameterProto(key=sys.argv[i][1:].replace("-", ""),
                                                              value=sys.argv[i + 1])
        params.append(param)

    myfile = open("algo-log.txt", 'a')
    myfile.write("created parameter protos\n")
    myfile.close()

    log.write("Open connection to grpc server via 127.0.0.1:8081\n")
    channel = grpc.insecure_channel("127.0.0.1:8081")
    stub = PCSBasedComponentParameter_pb2_grpc.PCSBasedOptimizerServiceStub(channel)

    cmp = PCSBasedComponentParameter_pb2.PCSBasedComponentProto(name="5", parameters=params)

    myfile = open("algo-log.txt", 'a')
    myfile.write("created proto comp, now evaluate\n")
    myfile.close()
    
    response = stub.Evaluate(cmp)
    myfile = open("algo-log.txt", 'a')
    myfile.write("evaluation done." + str(response)+"\n")
    myfile.close()
    
    channel.close()

    f = open("testout.txt", "a")
    f.write("### start run ###\n")
    for arg in sys.argv:
        f.write(arg + "\n")
    f.write("### end run ###\n\n")
    f.close()
    log.close()
    
    myfile = open("algo-log.txt", 'a')
    myfile.write('Result for SMAC: SUCCESS, -1, -1, %f, %s\n' % (response.result, "5"))
    myfile.close()
    
    print("\n")
    if(response.result >= 0):
        print("Result for SMAC: SUCCESS,-1,-1,"+str(response.result)+",5")
    elif (response.result == -1):
        print("Result for SMAC: TIMEOUT,-1,-1,-1,5")
    elif (response.result == -2):
        print("Result for SMAC: MEMOUT,-1,-1,-1,5")
    else:
        print("Result for SMAC: CRASHED,-1, -1,-1,5")
        
        