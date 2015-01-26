__author__ = 'sebastian'

"""
Script to retrieve the WordNet synsets given the WordNet-3.0 ID.
"""

import xml.etree.ElementTree as ET
from nltk.corpus import wordnet

def id2ss(id):
    """Given a WordNet Affect id (e.g. n#05588321) return a synset"""
    return wordnet._synset_from_pos_and_offset(str(id[:1]), int(id[2:]))

emotions = ["fear", "surprise", "anger", "anticipation", "joy", "sadness", "disgust", "trust"]

categ_tree = ET.parse('a-hierarchy_plutchik.xml')
categ_root = categ_tree.getroot()
# dictionary to save hierarchy information
categ_dict = {}
# populate dictionary
for categ in categ_root:
    categ_dict[categ.get('name')] = categ.get('isa')

tree = ET.parse('a-synsets-30.xml')
root = tree.getroot()
noun_dict = {}
with open("wn-affect-lemmas.txt", "a") as f:
    for pos in root:
        f.write("\n# {0}\n".format(pos.tag))
        for synset in pos:
            id = synset.get('id')
            if id[:1] == "n":
                categ = synset.get('categ')
                noun_dict[id] = categ
            else:
                categ = noun_dict[synset.get('noun-id')]
            while categ in categ_dict:
                print "{0} isa {1}".format(categ, categ_dict[categ])
                categ = categ_dict[categ]
            if categ not in emotions:
                continue
            synset = id2ss(id)
            lemmas = [str(lemma.name()) for lemma in synset.lemmas()]
            print "{0}\t{1}".format(categ, " ".join(lemmas))
            f.write("{0}\t{1}\n".format(categ, " ".join(lemmas)))
