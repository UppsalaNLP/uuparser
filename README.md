# UUParser: A transition-based dependency parser for Universal Dependencies

This parser is based on [Eli Kiperwasser's transition-based parser](http://github.com/elikip/bist-parser) using BiLSTM feature extractors.
We adapted the parser to Universal Dependencies and extended it as described in these papers:

* (Version 1.0) Adaptation to UD + removed POS tags from the input + added character vectors + use pseudo-projective:
>Miryam de Lhoneux, Yan Shao, Ali Basirat, Eliyahu Kiperwasser, Sara Stymne, Yoav Goldberg, and Joakim Nivre. 2017. From Raw Text to Universal Dependencies - Look, No Tags! In Proceedings of the CoNLL 2017 Shared Task: Multilingual Parsing from Raw Text to Universal Dependencies.

* (Version 2.0) Removed the need for pseudo-projective parsing by using a swap transition and creating a partially dynamic oracle as described in:
>Miryam de Lhoneux, Sara Stymne and Joakim Nivre. 2017. Arc-Hybrid Non-Projective Dependency Parsing with a Static-Dynamic Oracle. In Proceedings of the The 15th International Conference on Parsing Technologies (IWPT).

* (Version 2.3) Added POS tags back in, extended cross-treebank functionality and use of external embeddings and some tuning of default hyperparameters:

>Aaron Smith, Bernd Bohnet, Miryam de Lhoneux, Joakim Nivre, Yan Shao and Sara Stymne. 2018. 82 Treebanks, 34 Models: Universal Dependency Parsing with Cross-Treebank Models. In Proceedings of the CoNLL 2018 Shared Task: Multilingual Parsing from Raw Text to Universal Dependencies.

The techniques behind the original parser are described in the paper [Simple and Accurate Dependency Parsing Using Bidirectional LSTM Feature Representations](https://www.transacl.org/ojs/index.php/tacl/article/viewFile/885/198).

#### Required software

 * Python 3 (/!\ recent move from python 2.7 which was used for all releases).
 * [DyNet library](https://github.com/clab/dynet/tree/master/python)

    Note: the current version is Dynet 2.0 but Dynet 1.0 was used in both releases 1.0 and 2.0

### Installation

- (Python > 3.7 only) install [Cython](https://cython.org/) first in order to be able to install [DyNet](https://github.com/clab/dynet)
  
  ```console
  pip install cython
  ```

- Install with pip

  ```console
  pip install uuparser
  ```

Alternatively you can install directly from the master branch with `pip install git+https://github.com/UppsalaNLP/uuparser`.

#### Train a parsing model

To train a set of parsing models for a set of treebanks:

```console
uuparser --outdir [results directory] --datadir [directory of UD files with the structure UD\_\*\*/iso\_id-ud-train/dev.conllu] --include [treebanks to include denoted by their ISO id]
```

#### Options

The parser has numerous options to allow you to fine-control its behaviour. For a full list, type:

```console
uuparser --help
```

We recommend you set the --dynet-mem option to a larger number when running the full training procedure on larger treebanks.
Commonly used values are 5000 and 10000 (in MB).

Note that due to random initialization and other non-deterministic elements in the training process, you will not obtain the same results even when training twice under exactly the same circumstances (e.g. languages, number of epochs etc.).
To ensure identical results between two runs, we recommend setting the --dynet-seed option to the same value both times (e.g. --dynet-seed 123456789).
This ensures that Python's random number generator and Dynet both produce the same sequence of random numbers.

#### Example

The following is a typical command for training separate models for UD_Swedish, UD_Russian, and UD_English:

```console
uuparser --outdir my_output --datadir ud-treebanks-v2.0 --include "sv_talbanken en_partut ru_syntagrus" --dynet-seed 123456789 --dynet-mem 10000
```

The output files will be created in my_output/sv_talbanken, my_output/ru_syntagrus, and my_output/en_partut.
This command assumes that the directory UD_Swedish exists in ud-treebanks-v2.0 and contains at least the file sv-ud-train.conllu (and the same for the other two languages).
If dev data is also found (sv-ud-dev.conllu), model selection will be performed by default by parsing the dev data at each epoch and choosing the model from the epoch with the highest LAS.

#### Parse data with your parsing model

```console
uuparser --predict --outdir [results directory] --datadir [directory of UD files with the structure UD\_\*\*/iso\_id-ud-train/dev.conllu] --include [treebanks to include denoted by their ISO id]
```

By default this will parse the dev data for the specified languages with the model files (by default barchybrid.model) found in treebank-specific subdirectories of outdir.
Note that if you don't want to use the same directory for model files and output files, you can specify the --modeldir explictly.
By default, is it assumed that --modeldir is the same as --outdir.

#### Multi-treebank models

An important feature of the parser is the ability to train cross-treebank models by adding a treebank embedding.
Information about this technique is detailed in:

>Sara Stymne, Miryam de Lhoneux, Aaron Smith, Joakim Nivre. 2018. Parser Training with Heterogeneous Treebanks. In Proceedings of ACL.

To train a multi-treebank model, simply add the --multiling flag at both training and test time.
The output model files will be stored by default directly in the specified output directory rather than in treebank-specific subdirectories.

#### Using ELMo continous word embeddings

UUParser supports ELMo embeddings as an input to the LSTM. 
Specify a HDF5 file containing the layers for each word using the `--elmo` options.
The file must contain all sentences from a treebank and be tokenized according
to the gold segmentation. Additionally, UUParser expects all three layers to be present
 as the task-specific weights are learned during training.
 
The weights for the English treebanks EWT, GUM, and LinES can be downloaded [here](https://www.dropbox.com/sh/kyaq2mt07qpbtxt/AACD6LsEJrqgaURaAZ-fvHpoa?dl=0).

You can also specify the gamma scalar using `--elmo_gamma` or set `--elmo_learn_gamma`
to learn the value during training.

Credits to Johannes Gontrum for this addition.
Credits to Giuseppe Attardi for porting the parser to python 3.

#### Citation

If you make use of this software for research purposes, we'll appreciate if you cite the following:

If you use version 2.0 or later:

    @InProceedings{delhoneux17arc,
        author    = {Miryam de Lhoneux and Sara Stymne and Joakim Nivre},
        title     = {Arc-Hybrid Non-Projective Dependency Parsing with a Static-Dynamic Oracle},
        booktitle = {Proceedings of the The 15th International Conference on Parsing Technologies (IWPT).},
        year      = {2017},
        address = {Pisa, Italy}
    }

If you use version 1.0:

    @InProceedings{uu-conll17,
        author    = {Miryam de Lhoneux and Yan Shao and Ali Basirat and Eliyahu Kiperwasser and Sara Stymne and Yoav Goldberg and Joakim Nivre},
        title     = {From Raw Text to Universal Dependencies -- Look, No Tags!},
        booktitle = {Proceedings of the CoNLL 2017 Shared Task: Multilingual Parsing from Raw Text to Universal Dependencies. },
        year      = {2017},
        address = {Vancouver, Canada}
    }

And the original parser paper:

    @article{DBLP:journals/tacl/KiperwasserG16,
        author    = {Eliyahu Kiperwasser and Yoav Goldberg},
        title     = {Simple and Accurate Dependency Parsing Using Bidirectional {LSTM}
               Feature Representations},
        journal   = {{TACL}},
        volume    = {4},
        pages     = {313--327},
        year      = {2016},
        url       = {https://transacl.org/ojs/index.php/tacl/article/view/885},
        timestamp = {Tue, 09 Aug 2016 14:51:09 +0200},
        biburl    = {http://dblp.uni-trier.de/rec/bib/journals/tacl/KiperwasserG16},
        bibsource = {dblp computer science bibliography, http://dblp.org}
    }

#### License

This software is released under the terms of the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

#### Contact

For questions and usage issues, please contact miryam.de\_lhoneux@lingfil.uu.se
