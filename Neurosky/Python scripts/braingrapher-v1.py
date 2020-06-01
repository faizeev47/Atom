import socket
import json
import numpy as np
from matplotlib import pyplot as plt
from matplotlib.animation import FuncAnimation

thinkgear_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
try:
    thinkgear_socket.connect(('localhost',13854))
    thinkgear_socket.send(b'{"enableRawOutput": false,"format": "Json"}')
    thinkgear_socket.recv(1024)
except ConnectionRefusedError:
    print("Please connect the USB connector to computer and start the Thinkgear Connector.")
    exit(1)

x = np.arange(0, 50)
attention_line = np.zeros((50,), dtype=int)
attention = []
meditation_line = np.zeros((50,), dtype=int)
meditation = []

signalLevel = 0

fig, ax = plt.subplots()
def animate(i):
    try:
        packet = json.loads(thinkgear_socket.recv(1024).decode('UTF-8'))
        if ('eegPower' in packet):
            print("eegPower attributes:")
            for key, value in packet['eegPower'].items():
                print(f"\t{key}: {value}")
        if ('poorSignalLevel' in packet):
            signalLevel = int(packet['poorSignalLevel'])
        if ('eSense' in packet):
            attention_attr = packet['eSense']['attention']
            attention.insert(0, i)
            meditation_attr = packet['eSense']['meditation']
            meditation.insert(0, meditation_attr)
            print("eSense attributes:")
            print(f"\tAttention: {attention_attr}")
            print(f"\tMeditation: {meditation_attr}")
        else:
            attention.insert(0, 0)
            meditation.insert(0, 0)
            print("Scanning/Restablishing")
    except Exception as e:
        attention.insert(0, 0)
        meditation.insert(0, 0)
        print("Error:" + str(e))
    attention_line = np.zeros((50, ), dtype=int)
    meditation_line = np.zeros((50,), dtype=int)
    line_idx = 49
    for attr_idx in range(len(attention)):
        attention_line[line_idx] = attention[attr_idx]
        meditation_line[line_idx] = meditation[attr_idx]
        line_idx -= 1


    plt.cla()
    ax.set_title("Signal Level: " + str(signalLevel))
    plt.plot(x, attention_line, label="Attention")
    plt.plot(x, meditation_line, label="Meditation")
    plt.legend(loc="upper left")
    plt.tight_layout()

ani = FuncAnimation(plt.gcf(), animate, interval=1)

ax.set_title("Signal Level: " + str(signalLevel))
plt.plot(x, attention_line)
plt.plot(x, meditation_line)
plt.legend(loc="upper left")
plt.tight_layout()
plt.show()
