import socket
import json

bytearr = bytearray('{"enableRawOutput": false,"format": "Json"}', 'utf-8')

for byte in bytearr:
    print(byte, end="")
exit()

thinkgear_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
try:
    thinkgear_socket.connect(('localhost',13854))
    thinkgear_socket.send(b'{"enableRawOutput": false,"format": "Json"}')
    thinkgear_socket.recv(1024)

    while (True):
        try:
            packet = json.loads(thinkgear_socket.recv(1024).decode('UTF-8'))
            if ('eSense' in packet):
                
                print("eSense attributes: {}".format([packet['eSense']]))
            else:
                print("Scanning/Restablishing")
        except Exception:
            print("Error") 
except ConnectionRefusedError:
    print("Please connect the USB connector to computer and start the Thinkgear Connector.")
