import json
import numpy as np

import h5py


class ELMo(object):

    def __init__(self, elmo_file):
        print "Reading ELMo embeddings from '%s'" % elmo_file
        self.weights = h5py.File(elmo_file, 'r')

        self.sentence_to_index = json.loads(
            self.weights['sentence_to_index'][0])

        self.num_layers, _, self.emb_dim = self.weights['0'].shape

    def get_sentence_representation(self, sentence):
        """
        Looks up the sentence representation for the given sentence.
        :param sentence: String of space separated tokens.
        :return: ELMo.Sentence object
        """
        sentence_index = self.sentence_to_index.get(sentence)
        if not sentence_index:
            raise ValueError(
                "The sentence '%s' could not be found in the ELMo data."
                % sentence
            )

        return ELMo.Sentence(self.weights[sentence_index])

    class Sentence(object):

        def __init__(self, sentence_weights):
            self.sentence_weights = sentence_weights

        def __getitem__(self, i):
            """
            Returns the layers for the word at position i in the sentence.
            :param i: Index of the word.
            :return: (n x d) matrix where n is the number of layers
                     and d the number of embedding dimensions.
            """

            # self.sentence_weights is of dimensions (n x w x d)
            # with n as the number of layers
            # w the number of words
            # and d the number of dimensions.

            # Therefore, we must iterate over the matrix to retrieve the layer
            # for each word separately.

            layers = []
            for layer in self.sentence_weights:
                layers.append(layer[i])
            
            return np.array(layers)
