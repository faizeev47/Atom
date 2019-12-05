from matplotlib import pyplot as plt
from matplotlib.animation import FuncAnimation
from itertools import count
import numpy as np

# plt.style.use("dark_background")
#
# x1 = []
# y1 = []
#
# x2 = []
# y2 = []
#
# index = count()
#
# def animate(i):
#     next_val = next(index)
#     x1.append(next_val)
#     y1.append(random.randint(0,5))
#     x2.append(next_val)
#     y2.append(random.randint(0,5))
#
#     plt.cla()
#     plt.plot(x1, y1, label="Channel 1")
#     plt.plot(x2, y2, label="Channel 2")
#     plt.legend(loc="upper left")
#     plt.tight_layout()
#
# ani = FuncAnimation(plt.gcf(), animate, interval=1200)
#
# plt.plot(x1, y1)
#
# plt.tight_layout()
# plt.show()


fig, ax = plt.subplots()

x = np.arange(0, 2*np.pi, 0.01)
print(f"Numbers in x: {len(x)}")
line1, = ax.plot(x, np.sin(x))
line2, = ax.plot(x, np.sin(x))

def init():
    line1.set_ydata([np.nan] * len(x))
    line2.set_ydata([np.nan] * len(x))
    return line1, line2

def animate(i):
    y1 = np.sin(x + i / 100)
    y2 = np.sin(x - i / 100)
    line1.set_ydata(y1)
    line2.set_ydata(y2)
    print(f"i: {i}, len(y1): {len(y1)}, y1[0:10]: {y1[0:10]}")
    return line1, line2

ani = FuncAnimation(
        fig,
        animate,
        init_func=init,
        interval=1,
        blit=True,
        save_count=50)

plt.show()
