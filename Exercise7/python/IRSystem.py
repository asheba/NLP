#!/usr/bin/env python
import json
import math
import os
import re
import sys
import random

from collections import Counter
from PorterStemmer import PorterStemmer
from copy import deepcopy


class IRSystem:

    def __init__(self):
        # For holding the data - initialized in read_data()
        self.titles = []
        self.docs = []
        self.vocab = []
        # For the text pre-processing.
        self.alphanum = re.compile('[^a-zA-Z0-9]')
        self.p = PorterStemmer()


    def get_uniq_words(self):
        uniq = set()
        for doc in self.docs:
            for word in doc:
                uniq.add(word)
        return uniq


    def __read_raw_data(self, dirname):
        print "Stemming Documents..."

        titles = []
        docs = []
        os.mkdir('%s/stemmed' % dirname)
        title_pattern = re.compile('(.*) \d+\.txt')

        # make sure we're only getting the files we actually want
        filenames = []
        for filename in os.listdir('%s/raw' % dirname):
            if filename.endswith(".txt") and not filename.startswith("."):
                filenames.append(filename)

        for i, filename in enumerate(filenames):
            title = title_pattern.search(filename).group(1)
            print "    Doc %d of %d: %s" % (i+1, len(filenames), title)
            titles.append(title)
            contents = []
            f = open('%s/raw/%s' % (dirname, filename), 'r')
            of = open('%s/stemmed/%s.txt' % (dirname, title), 'w')
            for line in f:
                # make sure everything is lower case
                line = line.lower()
                # split on whitespace
                line = [xx.strip() for xx in line.split()]
                # remove non alphanumeric characters
                line = [self.alphanum.sub('', xx) for xx in line]
                # remove any words that are now empty
                line = [xx for xx in line if xx != '']
                # stem words
                line = [self.p.stem(xx) for xx in line]
                # add to the document's conents
                contents.extend(line)
                if len(line) > 0:
                    of.write(" ".join(line))
                    of.write('\n')
            f.close()
            of.close()
            docs.append(contents)

        return titles, docs


    def __read_stemmed_data(self, dirname):
        print "Already stemmed!"
        titles = []
        docs = []

        # make sure we're only getting the files we actually want
        filenames = []
        for filename in os.listdir('%s/stemmed' % dirname):
            if filename.endswith(".txt") and not filename.startswith("."):
                filenames.append(filename)

        if len(filenames) != 60:
            msg = "There are not 60 documents in ../data/RiderHaggard/stemmed/\n"
            msg += "Remove ../data/RiderHaggard/stemmed/ directory and re-run."
            raise Exception(msg)

        for i, filename in enumerate(filenames):
            title = filename.split('.')[0]
            titles.append(title)
            contents = []
            f = open('%s/stemmed/%s' % (dirname, filename), 'r')
            for line in f:
                # split on whitespace
                line = [xx.strip() for xx in line.split()]
                # add to the document's conents
                contents.extend(line)
            f.close()
            docs.append(contents)

        return titles, docs


    def read_data(self, dirname):
        """
        Given the location of the 'data' directory, reads in the documents to
        be indexed.
        """
        # NOTE: We cache stemmed documents for speed
        #       (i.e. write to files in new 'stemmed/' dir).

        print "Reading in documents..."
        # dict mapping file names to list of "words" (tokens)
        filenames = os.listdir(dirname)
        subdirs = os.listdir(dirname)
        if 'stemmed' in subdirs:
            titles, docs = self.__read_stemmed_data(dirname)
        else:
            titles, docs = self.__read_raw_data(dirname)

        # Sort document alphabetically by title to ensure we have the proper
        # document indices when referring to them.
        ordering = [idx for idx, title in sorted(enumerate(titles),
            key = lambda xx : xx[1])]

        self.titles = []
        self.docs = []
        numdocs = len(docs)
        for d in range(numdocs):
            self.titles.append(titles[ordering[d]])
            self.docs.append(docs[ordering[d]])

        # Get the vocabulary.
        self.vocab = [xx for xx in self.get_uniq_words()]

        # My additions - sorted documents
        self.sorted_docs = [sorted(doc) for doc in self.docs]
        self.reverse_sorted_docs = [doc[:] for doc in self.sorted_docs]
        map(list.reverse, self.reverse_sorted_docs)

    def compute_idf(self, word):
        """
        Computes the idf of the given word. Needs the inv_index set up - do this by calling self.index
        """
        N = len(self.docs)
        df = len(self.inv_index[word])
        return math.log(N*1.0/df,10)

    def compute_tfidf(self):
        # -------------------------------------------------------------------
        # TODO: Compute and store TF-IDF values for words and documents.
        #       Recall that you can make use of:
        #         * self.vocab: a list of all distinct (stemmed) words
        #         * self.docs: a list of lists, where the i-th document is
        #                   self.docs[i] => ['word1', 'word2', ..., 'wordN']
        #       NOTE that you probably do *not* want to store a value for every
        #       word-document pair, but rather just for those pairs where a
        #       word actually occurs in the document.
        print "Calculating tf-idf..."
        self.tfidf = {}

        for doc_id, doc in enumerate(self.docs):

            word_counter = Counter(self.docs[doc_id])
            for word in word_counter.keys():
                if word not in self.tfidf:
                    self.tfidf[word] = {}

                self.tfidf[word][doc_id] = 1 + math.log(word_counter[word], 10)

        for word in self.tfidf:
            idf = self.compute_idf(word)

            for doc_id in self.tfidf[word]:
                self.tfidf[word][doc_id] *= idf


    def get_tfidf(self, word, document):
        return self.tfidf[word][document]


    def get_tfidf_unstemmed(self, word, document):
        """
        This function gets the TF-IDF of an *unstemmed* word in a document.
        Stems the word and then calls get_tfidf. You should *not* need to
        change this interface, but it is necessary for submission.
        """
        word = self.p.stem(word)
        return self.get_tfidf(word, document)


    def index(self):
        """
        Build an index of the documents.
        """
        print "Indexing..."
        # ------------------------------------------------------------------
        # TODO: Create an inverted index.
        #       Granted this may not be a linked list as in a proper
        #       implementation.
        #       Some helpful instance variables:
        #         * self.docs = List of documents
        #         * self.titles = List of titles

        inv_index = {}
        for word in self.vocab:
            inv_index[word] = []

        for doc_id, doc in enumerate(self.docs):
            for word in sorted(list(set(doc))):
                inv_index[word].append(doc_id)

        
        self.inv_index = inv_index

        # ------------------------------------------------------------------


    def get_posting(self, word):
        """
        Given a word, this returns the list of document indices (sorted) in
        which the word occurs.
        """
        return self.inv_index[word]


    def get_posting_unstemmed(self, word):
        """
        Given a word, this *stems* the word and then calls get_posting on the
        stemmed word to get its postings list. You should *not* need to change
        this function. It is needed for submission.
        """
        word = self.p.stem(word)
        return self.get_posting(word)


    def boolean_retrieve(self, query):
        """
        Given a query in the form of a list of *stemmed* words, this returns
        the list of documents in which *all* of those words occur (ie an AND
        query).
        Return an empty list if the query does not return any documents.
        """

        postings = map(set,map(self.get_posting, query))
        matching_doc_ids = set(range(len(self.docs))).intersection(*postings)

        return sorted(matching_doc_ids)

    def calculate_score_by_jaccard(self, query):
        scores = [0.0 for xx in range(len(self.docs))]
        
        words_in_query = set(query)

        for d, doc in enumerate(self.docs):
            words_in_doc = set(doc)
            scores[d] = len(words_in_query.intersection(words_in_doc)) / float(len(words_in_query.union(words_in_doc)))

        return scores

    def normalized_length(self, doc_id):
        """
        Normalizes length of the given document: sums the squared TFIDF score of the 
        SET (meaning unique) of all words in the document and returns the square root of the sum.
        """
        word_wtf_in_doc = [self.tfidf[word][doc_id] for word in set(self.docs[doc_id])]
        return sum(map(lambda x: x**2, word_wtf_in_doc)) ** 0.5

    def calculate_cosine_scores(self, query):
        scores = [0.0 for xx in range(len(self.docs))]
        lengths = [self.normalized_length(doc_id) for doc_id in range(len(self.docs))]

        for term in set(query):
            query_weighted_term_frequency = 1 + math.log(query.count(term), 10)
            for doc_id in self.get_posting(term):
                scores[doc_id] += self.tfidf[term][doc_id] * query_weighted_term_frequency

        scores = [score/length for score,length in zip(scores,lengths)]
        return scores


    def rank_retrieve(self, query):
        """
        Given a query (a list of words), return a rank-ordered list of
        documents (by ID) and score for the query.
        """
        #scores = self.calculate_score_by_jaccard(query)
        scores = self.calculate_cosine_scores(query)

        ranking = [idx for idx, sim in sorted(enumerate(scores),
            key = lambda xx : xx[1], reverse = True)]
        results = []
        for i in range(10):
            results.append((ranking[i], scores[ranking[i]]))
        return results


    def process_query(self, query_str):
        """
        Given a query string, process it and return the list of lowercase,
        alphanumeric, stemmed words in the string.
        """
        # make sure everything is lower case
        query = query_str.lower()
        # split on whitespace
        query = query.split()
        # remove non alphanumeric characters
        query = [self.alphanum.sub('', xx) for xx in query]
        # stem words
        query = [self.p.stem(xx) for xx in query]
        return query


    def query_retrieve(self, query_str):
        """
        Given a string, process and then return the list of matching documents
        found by boolean_retrieve().
        """
        query = self.process_query(query_str)
        return self.boolean_retrieve(query)


    def query_rank(self, query_str):
        """
        Given a string, process and then return the list of the top matching
        documents, rank-ordered.
        """
        query = self.process_query(query_str)
        return self.rank_retrieve(query)


def run_tests(irsys):
    print "===== Running tests ====="

    ff = open('../data/queries.txt')
    questions = [xx.strip() for xx in ff.readlines()]
    ff.close()
    ff = open('../data/solutions.txt')
    solutions = [xx.strip() for xx in ff.readlines()]
    ff.close()

    epsilon = 1e-4
    for part in range(4):
        points = 0
        num_correct = 0
        num_total = 0

        prob = questions[part]
        soln = json.loads(solutions[part])

        if part == 0:     # inverted index test
            print "Inverted Index Test"
            words = prob.split(", ")
            for i, word in enumerate(words):
                num_total += 1
                posting = irsys.get_posting_unstemmed(word)
                if set(posting) == set(soln[i]):
                    num_correct += 1

        elif part == 1:   # boolean retrieval test
            print "Boolean Retrieval Test"
            queries = prob.split(", ")
            for i, query in enumerate(queries):
                num_total += 1
                guess = irsys.query_retrieve(query)
                if set(guess) == set(soln[i]):
                    num_correct += 1

        elif part == 2:   # tfidf test
            print "TF-IDF Test"
            queries = prob.split("; ")
            queries = [xx.split(", ") for xx in queries]
            queries = [(xx[0], int(xx[1])) for xx in queries]
            for i, (word, doc) in enumerate(queries):
                num_total += 1
                guess = irsys.get_tfidf_unstemmed(word, doc)
                if guess >= float(soln[i]) - epsilon and \
                        guess <= float(soln[i]) + epsilon:
                    num_correct += 1

        elif part == 3:   # cosine similarity test
            print "Cosine Similarity Test"
            queries = prob.split(", ")
            for i, query in enumerate(queries):
                num_total += 1
                ranked = irsys.query_rank(query)
                top_rank = ranked[0]
                if top_rank[0] == soln[i][0]:
                    if top_rank[1] >= float(soln[i][1]) - epsilon and \
                            top_rank[1] <= float(soln[i][1]) + epsilon:
                        num_correct += 1

        feedback = "%d/%d Correct. Accuracy: %f" % \
                (num_correct, num_total, float(num_correct)/num_total)
        if num_correct == num_total:
            points = 3
        elif num_correct > 0.75 * num_total:
            points = 2
        elif num_correct > 0:
            points = 1
        else:
            points = 0

        print "    Score: %d Feedback: %s" % (points, feedback)


def main(args):
    irsys = IRSystem()
    irsys.read_data('../data/RiderHaggard')
    irsys.index()
    irsys.compute_tfidf()
    
    if len(args) == 0:
        run_tests(irsys)
    else:
        query = " ".join(args)
        print "Best matching documents to '%s':" % query
        results = irsys.query_rank(query)
        for docId, score in results:
            print "%s: %e" % (irsys.titles[docId], score)


if __name__ == '__main__':
    args = sys.argv[1:]
    main(args)
