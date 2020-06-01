import json
from json.decoder import JSONDecodeError

# with open('log.json') as f:
#     for line in f:
#         try:
#             obj = json.loads(line)
#             if 'eSense' in obj:
#                 print(json.dumps(obj))
#         except JSONDecodeError:
#             pass

with open('logF.json') as f:
    for line in f:
        obj = json.loads(line)
        eeg = obj["eegPower"]
        print("{}, {}, {}, {}, {}, {}, {}, {}, {}, {}".format(
            obj["poorSignalLevel"],
            eeg["delta"], eeg["theta"], eeg["lowAlpha"],
            eeg["highAlpha"], eeg["lowBeta"], eeg["highBeta"],
            eeg["lowGamma"], eeg["highGamma"], obj["eSense"]["attention"]))