import dynet as dy

class MLP(object):
    def __init__(self, model=None, in_dim, hid_dim, out_dim, activation=dy.tanh):
        ##
        self._W1 = model.add_parameters((hid_dim, in_dim))
        self._b1 = model.add_parameters(hid_dim)
        self._W2 = model.add_parameters((out_dim, hid_dim))
        self._b2 = model.add_parameters(out_dim)

        #WORKS
        #self.model = dy.ParameterCollection() # this works
        #self._W1 = self.model.add_parameters((hid_dim, in_dim))
        #self._b1 = self.model.add_parameters(hid_dim)
        #self._W2 = self.model.add_parameters((out_dim, hid_dim))
        #self._b2 = self.model.add_parameters(out_dim)

        self.activation = activation

    def hid_layer(self,x):
        W = dy.parameter(self._W1)
        b = dy.parameter(self._b1)
        return self.activation(W*x+b)

    def out_layer(self,x):
        W = dy.parameter(self._W2)
        b = dy.parameter(self._b2)
        return self.activation(W*x+b)

    def __call__(self,x):
        h = self.hid_layer(x)
        return self.out_layer(h)
