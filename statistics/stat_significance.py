import pandas as pd
import sys
from numpy import mean, minimum
from math import sqrt

"""
#############################
Implementation of statistical significance tests.
Currently available t-test for comparing two systems
cross-validation results. Only 5 and 10 folds supported.
Further reference values would be necessary to support 
more folds.

Implementation based on: 
 
[1] Machine Learning, Tom Mitchell, McGraw Hill, 1997. (pp. 145--148)
[2] Evaluating Learning Algorithms : A Classification Perspective, Nathalie 
    Japkowicz and Mohak Shah, Cambridge University Press 2011 (pp. 217--220)

Levels of significance extracted from [2, p. 252]
"""
LEVEL_OF_SIGNIFICANCE= {
    # 5 folds
    4 : {40: 0.2707,
         30: 0.5686,
         25: 0.7407,
         20: 0.9410,
         15: 1.190,
         10: 1.533,
         5: 2.132,
         2.5: 2.776,
         1: 3.747,
         0.5: 4.604, #higher than this means that the systems differ with a confidence level of 99.5% 
         0.1: 7.173,
         0.05:8.610
         },  
    # 10 folds                  
    9 : {40: 0.2610,
         30: 0.5435,
         25: 0.7027,
         20: 0.8834,
         15: 1.100,
         10: 1.383,
         5: 1.833,
         2.5: 2.262,
         1: 2.821,
         0.5: 3.250,
         0.1: 4.297,
         0.05:4.781
         }
    }


def main():
    if len(sys.argv) != 2:
        print "Usage:", sys.argv[0], "<csv file>"
        print "The csv file contains two columns with the evaluation of the " +\
                "two systems; they should include a header"
        sys.exit()
    else:
        sys1, sys2 = read_csv(sys.argv[1])
        t_value = compute_t_statistic(sys1, sys2)    
        is_statistically_significant(len(sys1), t_value)
        
def read_csv(csv_file): 
    """Opens the csv file and loads the values from the two expected columns.
    The first row is considered as the header. Obviously, both columns should 
    contain the same amount of values"""   
    df = pd.read_csv(csv_file, header=0, names=[1,2])
    print df
    sys1 = df[1].values
    sys2 = df[2].values
    return sys1, sys2    

def compute_t_statistic(values1, values2):
    """
    Return the t statistic for the two lists of performance/error values.
    
    Parameters
    ----------
    values[1,2] : array_like
        Array containing performance/error of the two systems in the different
        folds.
        
    Returns
    -------
    t : float 
        The t statistic for the two lists of performance/error values.  
    """
    
    delta = 0.0
    diff = []
    
    mean_s1 = mean(values1)
    mean_s2 = mean(values2)
    diff = [a-b for a,b in zip(values1,values2)]
    delta = mean(diff)
    s=0.0
    for i in range(0, len(values1)):
        s+=(diff[i]-delta)**2   
        
    s= sqrt( s / (len(values1)* (len(values1)-1)) )           
    print 's = ', s    
    t = (mean_s1 - mean_s2) / (s/sqrt(len(values1)))
    print 't = ', t
    return t

def is_statistically_significant(folds, t):
    """
    Explores the table of percentage points of the t distribution and 
    determines the confidence level of the statistical significance.
    
    Parameters
    ----------
    folds : int
        number of folds
    t : float
        estimated t
        
    Returns
    -------
    null
    
    See Also
    --------
    compute_t_statistic()
    
    """
    if not LEVEL_OF_SIGNIFICANCE.has_key(folds-1):
        print "I do not have reference values to estimate significance. Check it manually with t"
    
    vals = LEVEL_OF_SIGNIFICANCE.get(folds-1)
    min = float("inf")
    
    for key, thres in vals.iteritems():
        if t >= thres:
            min = minimum(min, key) 
    print "Statistical significance with ", 100-min, "confidence"

if __name__ == "__main__":
    main()