import re

with open('text3.txt') as text:
	for line in text:
		if (not re.search("<  [0-9]*  >", line)):
			print(line)
