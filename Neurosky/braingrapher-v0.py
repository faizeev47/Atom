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

fig, ax = plt.subplots()
ax.get_xaxis().set_visible(False)

x = np.arange(0, 50)
attention = []
attention_line, = ax.plot(x, (np.zeros((50, ), dtype=int)), label="Attention")

def init():
    attention_line.set_ydata([np.nan] * len(x))
    return attention_line,

def animate(i):
    try:
        packet = json.loads(thinkgear_socket.recv(1024).decode('UTF-8'))
        if ('eSense' in packet):
            attention_attr = packet['eSense']['attention']
            attention.append(i)

            print("eSense attributes:")
            print(f"\tAttention: {packet['eSense']['attention']}")
        else:
            attention.append(0)
            print("Scanning/Restablishing")
    except Exception as e:
        attention.append(0)
        print("Error:" + str(e))
    y1 = np.zeros((50, ), dtype=int)
    y1_idx = 0
    for attn_idx in range(len(attention) - 1, -1, -1):
        y1[y1_idx] = attention[attn_idx]
        y1_idx += 1
    print(attention)
    print(y1)
    attention_line.set_ydata(y1)
    ax.set_ylim(np.amin(y1), np.amax(y1))
    ax.set_yticks(y1)

    return attention_line,

ani = FuncAnimation(
        fig,
        animate,
        init_func=init,
        interval=1,
        blit=True,
        save_count=50)

plt.show()
