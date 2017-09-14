# UUParser
## Transition based dependency parser for Universal Dependencies using BiLSTM feature extractors.
This parser is based on [Eli Kiperwasser's transition-based parser](http://github.com/elikip/bist-parser).

We adapted the parser to Universal Dependencies as well as extended it as described in these papers:

* (Version 1.0) Adaptation to UD + removed POS tags from the input + added character vectors + use pseudo-projective:
>Miryam de Lhoneux, Yan Shao, Ali Basirat, Eliyahu Kiperwasser, Sara Stymne, Yoav Goldberg, and Joakim Nivre. 2017. From raw text to Universal Dependencies - look, no tags! In Proceedings of the CoNLL 2017 Shared Task: Multilingual Parsing from Raw Text to Universal Dependencies. Association for Computational Linguistics.


* (Version 2.0) Removed the need for pseudo-projective parsing by using swap and creating a partially dynamic oracle as described in:
>Miryam de Lhoneux, Sara Stymne and Joakim Nivre. 2017. Arc-Hybrid Non-Projective Dependency Parsing with a Static-Dynamic Oracle. In Proceedings of the The 15th International Conference on Parsing Technologies (IWPT).

The techniques behind the original parser are described in the paper [Simple and Accurate Dependency Parsing Using Bidirectional LSTM Feature Representations](https://www.transacl.org/ojs/index.php/tacl/article/viewFile/885/198). 

#### Required software

 * Python 2.7 interpreter
 * [DyNet library](https://github.com/clab/dynet/tree/master/python)

    Note: the current version is Dynet 2.0 but dynet 1.0 was used in both releases 1.0 and 2.0


#### Train a parsing model

To train a set of parsing models for a set of treebanks:

python src/parser.py --dynet-seed 123456789 --outdir [results directory] --datadir [directory of UD files with the structure UD\_\*\*/iso\_id-ud-train/dev.conllu] --include [languages to include denoted by their ISO id] --epochs 30 --userlmost --dynet-mem 5000 --extrn [external word embeddings file]

For optimal results you should add the following to the command prompt `--k 3 --usehead --userl`. These switch will set the stack to 3 elements; use the BiLSTM of the head of trees on the stack as feature vectors; and add the BiLSTM of the right/leftmost children to the feature vectors.

#### Pick a model
These commands save one model per epoch and evaluates it on the dev set.
For prediction, the parser expects a model directory with one model and a parameter file for each language in a subdirectory.
E.g.: models/en/barchybrid.model | models/en/params.pickle
Before using your models for prediction, you probably want to pick one model per language. You can do this manually by looking at performance on the dev sets and copying the model and parameter files.
Alternatively, you can use a script to do it for many languages at a time. This script expects a file with one iso code per line and a directory of trained models with evaluation on the dev sets. It creates a directory called 'models' and copies the best performing model on the dev set as measured by LAS. If models are found but no evaluation (some treebanks do not have a dev set), it picks the last epoch trained.

```
python scripts/pick_model.py iso_codes.txt dir_with_models
```

#### Parse data with your parsing model


##### Input similar to the shared task setup (a list of conllu files with a metadata.json file describing their content)

python src/parser.py --predict --outdir [results directory] --modeldir [a directory containing one model per language] --datadir [input directory] --include [languages to include denoted by their ISO id] --pseudo-proj --shared_task --shared_task_datadir [the shared task input directory] --dynet-mem 5000

##### Input has the same structure as the training data, and we take the dev files

python src/parser.py --predict --outdir [results directory] --modeldir [a directory containing one model per language] --datadir [directory of UD files with the structure UD\_\*\*/iso\_id-ud-train/dev.conllu] --include [languages to include denoted by their ISO id] --dynet-mem 5000

The parser will store the resulting conll file in the out directory (`--outdir`).

#### Citation

If you make use of this software for research purposes, we'll appreciate if you cite the following:

If you use version 2.0:

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
