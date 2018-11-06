import json
import numpy as np
import dynet as dy
import h5py


class ELMo(object):

    def __init__(self, elmo_file):
        print "Reading ELMo embeddings from '%s'" % elmo_file
        self.sentence_data = h5py.File(elmo_file, 'r')
        self.weights = []

        self.sentence_to_index = json.loads(
            self.sentence_data['sentence_to_index'][0])

        self.num_layers, _, self.emb_dim = self.sentence_data['0'].shape

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

        return ELMo.Sentence(self.sentence_data[sentence_index], self)

    def init_weights(self, model):
        self.weights = [
            model.add_parameters(
                1,
                init=1.0 / self.num_layers,
                name="elmo-weight-%s" % i
            )
            for i in range(self.num_layers)
        ]

    class Sentence(object):

        def __init__(self, sentence_weights, elmo):
            self.sentence_weights = sentence_weights
            self.elmo = elmo

        def __getitem__(self, i):
            """
            Return the weighted layers for the current word.
            :param i: Word at index i in the sentence.
            :return: Embedding for the word
            """
            layers = self._get_sentence_layers(i)

            y_hat = [
                dy.inputTensor(layer) * weight
                for layer, weight in zip(layers, self.elmo.weights)
            ]

            # Sum the layer contents together
            return dy.esum(y_hat)

        def _get_sentence_layers(self, i):
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