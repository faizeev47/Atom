import socket
import json
import numpy as np

thinkgear_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
try:
    thinkgear_socket.connect(('localhost',13854))
    thinkgear_socket.send(b'{"enableRawOutput": false,"format": "Json"}')
    thinkgear_socket.recv(1024)

    x = np.zeros((1000,) dtype=int)

    while (True):
        try:
            packet = json.loads(thinkgear_socket.recv(1024).decode('UTF-8'))
            if ('eSense' in packet):
                print("eSense attributes:")
                print(f"\tAttention: {packet['eSense']['attention']}")
                print(f"\tMeditation: {packet['eSense']['meditation']}")
            if ('eegPower' in packet):
                print("eegPower attributes:")
                for key, value in packet['eegPower'].items():
                    print(f"\t{key}: {value}")
            else:
                print("Scanning/Restablishing")
        except Exception:
            print("Error")
except ConnectionRefusedError:
    print("Please connect the USB connector to computer and start the Thinkgear Connector.")
