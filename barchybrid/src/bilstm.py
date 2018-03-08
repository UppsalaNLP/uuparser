import dynet as dy

class BiLSTM(object):
    def __init__(self,in_dim,out_dim,model,dropout_rate=None):
        self.dropout_rate = dropout_rate
        self.surfaceBuilders = [dy.VanillaLSTMBuilder(1, in_dim, out_dim, model),
                                dy.VanillaLSTMBuilder(1, in_dim, out_dim, model)]

    def set_token_vecs(self,sequence,dropout):
        """
        Get the forward and backward vectors of tokens in a sequence
        and concatenate them
        The token objects have a .vec attribute which gets updated
        @param: sequence is a list of objects that have a .vec attribute which
        is a vector
        """
        if dropout and self.dropout_rate is not None:
            self.surfaceBuilders[0].set_dropout(self.dropout_rate)
            self.surfaceBuilders[1].set_dropout(self.dropout_rate)
        else:
            self.surfaceBuilders[0].set_dropout(0)
            self.surfaceBuilders[1].set_dropout(0)


        forward  = self.surfaceBuilders[0].initial_state()
        backward = self.surfaceBuilders[1].initial_state()

        for ftoken, rtoken in zip(sequence, reversed(sequence)):
            forward = forward.add_input( ftoken.vec )
            backward = backward.add_input( rtoken.vec )
            ftoken.fvec = forward.output()
            rtoken.bvec = backward.output()

        for token in sequence:
            token.vec = dy.concatenate( [token.fvec, token.bvec] )

    def get_sequence_vector(self,sequence,dropout):
        """
        Pass a sequence of vectors through the BiLSTM. Return the sequence
        vector.
        @param: sequence is a list of vectors
                dropout is a boolean
        """
        if dropout:
            self.surfaceBuilders[0].set_dropout(self.dropout_rate)
            self.surfaceBuilders[1].set_dropout(self.dropout_rate)
        else:
            self.surfaceBuilders[0].set_dropout(0)
            self.surfaceBuilders[1].set_dropout(0)
        forward  = self.surfaceBuilders[0].initial_state()
        backward = self.surfaceBuilders[1].initial_state()

        for ftoken, rtoken in zip(sequence, reversed(sequence)):
            forward = forward.add_input( ftoken )
            backward = backward.add_input( rtoken )

        return dy.concatenate([forward.output(), backward.output()])

