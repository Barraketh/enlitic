#!/usr/bin/env python

import numpy as np
import os
import sys
import subprocess

from pprint import pprint
from sklearn.cross_validation import train_test_split
from sklearn.datasets import load_files
from sklearn.externals import joblib
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.grid_search import GridSearchCV
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from time import time

MODEL_DIR = 'data/model'
MODEL_NAME = 'model.pkl'


def grid_search():
    train, test, y_train, y_test = train_test_split(all_data.data, all_data.target, test_size=0.2)

    pipeline = Pipeline([
        ('vect', CountVectorizer(stop_words='english')),
        ('lr', LogisticRegression(solver='lbfgs', class_weight='balanced', C=10000))
    ])

    parameters = {
        # 'vect__max_df': (0.5, 0.75, 1.0),
        # 'vect__ngram_range': ((1, 1), (1, 2)),  # unigrams or bigrams
        # 'tfidf__use_idf': (True, False),
        # 'tfidf__norm': ('l1', 'l2'),
        # 'lr__class_weight': (None, 'balanced'),
        # 'lr__multi_class': ('ovr', 'multinomial'),
        # 'lr__C': np.logspace(3, 5, 5),
    }

    search = GridSearchCV(pipeline, parameters, n_jobs=-1, verbose=1, cv=5)

    print("Performing grid search...")
    print("pipeline:", [name for name, _ in pipeline.steps])
    print("parameters:")
    pprint(parameters)
    t0 = time()
    search.fit(train, y_train)
    print("done in %0.3fs" % (time() - t0))
    print()

    print("Best score: %0.3f" % search.best_score_)
    print("Best parameters set:")
    best_parameters = search.best_estimator_.get_params()
    for param_name in sorted(parameters.keys()):
        print("\t%s: %r" % (param_name, best_parameters[param_name]))

    print(search.score(test, y_test))

    return search


def train_optimal():
    cv = CountVectorizer(stop_words='english')
    cv.fit(all_data.data)
    X = cv.transform(all_data.data)
    lr = LogisticRegression(solver='lbfgs', class_weight='balanced', C=10000)
    lr.fit(X, all_data.target)
    visualize_coefficients(lr, cv.get_feature_names())
    return {'lr': lr, 'cv': cv, 'dict': all_data.target_names}


def visualize_coefficients(classifier, feature_names, n_top_features=25):
    # get coefficients with large absolute values
    for cat in classifier.classes_:
        print("******************************************************************")
        print(all_data.target_names[cat])
        print("******************************************************************")
        cat_coef = classifier.coef_[cat]
        positive_coefficients = np.argsort(cat_coef)[-n_top_features:]
        for c in positive_coefficients:
            print(feature_names[c])
        print("")


def save_model(model_dict):
    if not os.path.exists(MODEL_DIR):
        os.makedirs(MODEL_DIR)
    joblib.dump(model_dict, MODEL_DIR + '/' + MODEL_NAME)


def load_model():
    return joblib.load(MODEL_DIR + '/' + MODEL_NAME)


def predict_prob(model_dict, url):
    text = subprocess.check_output(["./artifacts/wiki-scraper-0.1", "enlitic.CategoryDownloader", url])
    featurized = model_dict['cv'].transform([text])
    prob = model_dict['lr'].predict_proba(featurized)
    for idx, val in enumerate(prob[0]):
        print model_dict['dict'][idx] + ": %.2f" % val


if __name__ == "__main__":
    if len(sys.argv) >= 2:
        command = sys.argv[1]
    else:
        raise Exception("Invalid number of arguments")

    if command == 'train':
        subprocess.call(["./artifacts/wiki-scraper-0.1", "enlitic.CategoryDownloader"])
        all_data = load_files('data/txt')
        optimal = train_optimal()
        save_model(optimal)
    elif command == 'test':
        model = load_model()
        predict_prob(model, sys.argv[2])
