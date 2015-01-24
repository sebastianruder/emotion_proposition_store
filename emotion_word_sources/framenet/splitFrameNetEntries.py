"""
Small script to separate lexical entries from FrameNet.
Takes read-file as 1st, write-file as 2nd arg.
"""

import sys

with open(sys.argv[1], "r") as doc:
	with open(sys.argv[2], "a") as doc2:
		for line in doc.readlines():
			lineList = line.strip("\n").split(" ")
			for item in lineList:
				doc2.write(item.split(".")[0] + "\n")

