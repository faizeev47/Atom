import socket
import json
from matplotlib import pyplot as plt
from matplotlib.animation import FuncAnimation
import numpy as np

thinkgear_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
try:
    thinkgear_socket.connect(('localhost',13854))
except ConnectionRefusedError:
    print("Please connect the USB connector to computer and start the Thinkgear Connector.")
thinkgear_socket.send(b'{"enableRawOutput": false,"format": "Json"}')
thinkgear_socket.recv(1024)

fig, ax = plt.subplots()

attention = []
meditation = []

x = np.arange(0, 50, 1)

line1, = ax.plot(x, np.zeros((len(x),), dtype=int))

def init():
    line1.set_ydata([np.nan] * len(x))
    return line1, line2

def animate(i):
    try:
        bytes = thinkgear_socket.recv(1024)
        data = bytes.decode('UTF-8')
        packet = json.loads(data)
        if ('eSense' in packet):
            print("eSense attributes:")
            attn_attr = packet['eSense']['attention']
            attention.append(attn_attr)
            print(f"\tAttention: {attn_attr}")
            medi_attr = packet['eSense']['meditation']
            meditation.append(medi_attr)
            print(f"\tMeditation: {medi_attr}")
            y1 = np.ones((len(x), ), dtype=int)
            line1.set_ydata(y1)
            y2 = np.ones((len(x), ), dtype=int)
            line2.set_ydata(y2)
        # if ('eegPower' in packet):
        #     print("eegPower attributes:")
        #     for key, value in packet['eegPower'].items():
        #         print(f"\t{key}: {value}")
        else:
            print("Scanning/Restablishing")
            y1 = np.zeros((len(x), ), dtype=int)
            line1.set_ydata(y1)
            y2 = np.zeros((len(x), ), dtype=int)
            line2.set_ydata(y2)
    except Exception as e:
        print("Error: " + str(e))


    return line1, line2

ani = FuncAnimation(
            fig,
            animate,
            init_func=init,
            interval=2,
            blit=True,
            save_count=50)

plt.show()
