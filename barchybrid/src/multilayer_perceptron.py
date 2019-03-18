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

class biMLP(MLP):
    """
    MLP that takes two inputs
    """
    def __init__(self, model, in_dim, hid_dim, hid2_dim, out_dim, activation=dy.tanh):
        self._W1_h = model.add_parameters((hid_dim, in_dim) )
        self._b1 = model.add_parameters(hid_dim)
        self._W1_d = model.add_parameters((hid_dim, in_dim) )
        self.has_2_layers = False
        if hid2_dim > 0:
            self.has_2_layers = True
            self._W12 = model.add_parameters((hid_dim,hid_dim))
            self._b12 = model.add_parameters((hid_dim))
        self._W2 = model.add_parameters((out_dim, hid_dim))
        self._b2 = model.add_parameters(out_dim)
        self.useDropout=False
        self.activation = activation

    def hid_layer(self,x,y,dropout):
        if dropout:
            W_h = dy.dropout(dy.parameter(self._W1_h),0.3)
            W_d = dy.dropout(dy.parameter(self._W1_d),0.3)
            b = dy.dropout(dy.parameter(self._b1),0.3)
        else:
            W_h = dy.parameter(self._W1_h)
            W_d = dy.parameter(self._W1_d)
            b = dy.parameter(self._b1)
        return self.activation(W_h*x+W_d*y+b)

    def __call__(self,h, d):
        h= self.hid_layer(h,d, self.useDropout)
        if self.has_2_layers:
            h2 = self.hid_2_layer(h,self.useDropout)
            h = h2
        return self.out_layer(h,self.useDropout)

