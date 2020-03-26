import numpy as np
import sys
from optparse import OptionParser

# example usage python analysis_multimono.py baselines_v2 3 byte_model 1 ar en fi
# find average improvement on best dev epochs for second experiment over first on the listed languages
# number after experiment name is the number of repeats of that experiment, the script
# expects to find in this case baselines_v2-2 and baselines_v3-3 and uses all to 
# calculate averages

def main(options,args):

    np.set_printoptions(precision=2)

    # if experiment/baseline name is e.g baseline_v3, need multiple runs stored as baselines_v3-2, baselines_v3-3, etc
    baseline = args[0] # name of baseline experiments
    bas_runs = int(args[1]) # number of baseline experiments
    exp_name = args[2] # name of new experiments
    exp_runs = int(args[3]) # number of new experiments
    print("Results for experiment: " + exp_name)

    langs = args[4:]
    bas_means = np.zeros((len(langs),))
    exp_means = np.zeros((len(langs),))

    for lang_counter in range(len(langs)):
        bas_means[lang_counter] = get_lang_mean((baseline,"Bas"), langs[lang_counter], bas_runs, options)
        exp_means[lang_counter] = get_lang_mean((exp_name,"Exp"), langs[lang_counter], exp_runs, options)
        print("Gain: %.2f"%(exp_means[lang_counter]-bas_means[lang_counter]))

    bas_mean = np.mean(bas_means)
    exp_mean = np.mean(exp_means)
    print("Means across all %i languages: Bas %.2f, Exp %.2f, Gain %.2f" %(len(langs),bas_mean, exp_mean, exp_mean - bas_mean))

def get_lang_mean(exp_name, lang, no_runs, options):
    if options.final_epochs:
        print("%s: mean of last %i epochs from %i runs for %s: " %(exp_name[1], options.no_epochs, no_runs, lang),end='')
    else:
        print("%s: mean of best %i epochs from %i runs for %s: " %(exp_name[1], options.no_epochs, no_runs, lang),end='')
    lang_means = np.zeros((no_runs,))
    for ind in range(1,no_runs+1): # loop over other baseline experiments
        if ind==1:
            scores_file = "./%s/%s/%s_scores.txt"%(exp_name[0],lang,lang)
        else:
            scores_file = "./%s-%i/%s/%s_scores.txt"%(exp_name[0],ind,lang,lang)
        scores = np.loadtxt(scores_file)
        if not options.final_epochs:
            scores = np.sort(scores)
        run_mean = np.mean(scores[-options.no_epochs:])
        lang_means[ind-1] = run_mean
        print("%.2f "%run_mean,end='')
    lang_mean = np.mean(lang_means)
    print("(%.2f)"%lang_mean)
    return lang_mean

if __name__ == "__main__":

    parser = OptionParser()
    parser.add_option("--no-epochs", type="int", metavar="INTEGER", default=5, help='Number of epochs to use')
    parser.add_option("--final-epochs", action="store_true", default=False, help='Use final rather than best epochs')
    (options, args) = parser.parse_args()

    main(options,args)
