import dynet as dy

class MLP(object):
    def __init__(self, model, name, in_dim, hid_dim, hid2_dim, out_dim, activation=dy.tanh):
        self.name=name
        self._W1 = model.add_parameters((hid_dim, in_dim), name='W1'+self.name)
        self._b1 = model.add_parameters(hid_dim,name='b1'+self.name)
        self.has_2_layers = False
        if hid2_dim > 0:
            self.has_2_layers = True
            self._W12 = model.add_parameters((hid_dim,hid_dim),name='W12' +
                                             self.name)
            self._b12 = model.add_parameters((hid_dim),name='b12' + self.name)
        self._W2 = model.add_parameters((out_dim, hid_dim),name='W2'+self.name)
        self._b2 = model.add_parameters(out_dim,name='b2'+self.name)
        #TODO: I think I've tried using it but maybe try again
        self.useDropout=False
        self.activation = activation

    def hid_layer(self,x,dropout):
        if dropout:
            W = dy.dropout(dy.parameter(self._W1),0.3)
            b = dy.dropout(dy.parameter(self._b1),0.3)
        else:
            W = dy.parameter(self._W1)
            b = dy.parameter(self._b1)
        return self.activation(W*x+b)

    def hid_2_layer(self,x,dropout):
        if dropout:
            W = dy.dropout(dy.parameter(self._W12),0.3)
            b = dy.dropout(dy.parameter(self._b12),0.3)
        else:
            W = dy.parameter(self._W12)
            b = dy.parameter(self._b12)
        return self.activation(W*x+b)

    def out_layer(self,x,dropout):
        if dropout:
            W = dy.dropout(dy.parameter(self._W2),0.3)
            b = dy.dropout(dy.parameter(self._b2),0.3)
        else:
            W = dy.parameter(self._W2)
            b = dy.parameter(self._b2)
        return (W*x+b)

    def __call__(self,x):
        h = self.hid_layer(x,self.useDropout)
        if self.has_2_layers:
            h2 = self.hid_2_layer(h,self.useDropout)
            h = h2
        return self.out_layer(h,self.useDropout)

