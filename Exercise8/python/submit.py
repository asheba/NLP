import urllib
import urllib2
import hashlib
import random
import email
import email.message
import email.encoders

def submit(partId):
  print '==\n== [nlp-class] Submitting Solutions | Programming Exercise %s\n=='% homework_id()
  if(not partId):
    partId = promptPart()

  partNames = validParts()
  if not isValidPartId(partId):
    print '!! Invalid homework part selected.'
    print '!! Expected an integer from 1 to %d.' % (len(partNames) + 1)
    print '!! Submission Cancelled'
    return

  (login, password) = loginPrompt()
  if not login:
    print '!! Submission Cancelled'
    return

  print '\n== Connecting to nlp-class ... '

  # Setup submit list
  if partId == len(partNames) + 1:
    submitParts = range(1, len(partNames) + 1)
  else:
    submitParts = [partId]

  for partId in submitParts:
    # Get Challenge
    (login, ch, state, ch_aux) = getChallenge(login, partId)
    if((not login) or (not ch) or (not state)):
      # Some error occured, error string in first return element.
      print '\n!! Error: %s\n' % login
      return

    # Attempt Submission with Challenge
    ch_resp = challengeResponse(login, password, ch)
    (result, string) = submitSolution(login, ch_resp, partId, output(partId, ch_aux), \
                                    source(partId), state, ch_aux)
    print '\n== [nlp-class] Submitted Homework %s - Part %d - %s' % \
          (homework_id(), partId, partNames[partId - 1]),
    print '== %s' % string.strip()



def promptPart():
  """Prompt the user for which part to submit."""
  print('== Select which part(s) to submit: ' + homework_id())
  partNames = validParts()
  srcFiles = sources()
  for i in range(1,len(partNames)+1):
    print '==   %d) %s [ %s ]' % (i, partNames[i - 1], srcFiles[i - 1])
  print '==   %d) All of the above \n==\nEnter your choice [1-%d]: ' % \
          (len(partNames) + 1, len(partNames) + 1)
  selPart = raw_input('')
  partId = int(selPart)
  if not isValidPartId(partId):
    partId = -1
  return partId


def validParts():
  """Returns a list of valid part names."""

  partNames = [
                'Development Wiki', \
                'Testing Wiki', \
                'Development Googling', \
                'Testing Googling'
              ]
  return partNames


def sources():
  """Returns source files, separated by part. Each part has a list of files."""
  srcs = [
           [ 'Wiki.py'], \
           [ 'Wiki.py'], \
           [ 'Googling.py'], \
           [ 'Googling.py']
         ]
  return srcs

def isValidPartId(partId):
  """Returns true if partId references a valid part."""
  partNames = validParts()
  return (partId and (partId >= 1) and (partId <= len(partNames) + 1))


# =========================== LOGIN HELPERS ===========================

def loginPrompt():
  """Prompt the user for login credentials. Returns a tuple (login, password)."""
  (login, password) = basicPrompt()
  return login, password


def basicPrompt():
  """Prompt the user for login credentials. Returns a tuple (login, password)."""
  login = raw_input('Login (Email address): ')
  password = raw_input('Password: ')
  return login, password


def homework_id():
  """Returns the string homework id."""
  return '8'


def getChallenge(email, partId):
  """Gets the challenge salt from the server. Returns (email,ch,state,ch_aux)."""
  url = challenge_url()
  values = {'email_address' : email, 'assignment_part_sid' : "%s-%d" % (homework_id(), partId), 'response_encoding' : 'delim'}
  data = urllib.urlencode(values)
  req = urllib2.Request(url, data)
  response = urllib2.urlopen(req)
  text = response.read().strip()

  # text is of the form email|ch|signature
  splits = text.split('|')
  if(len(splits) != 9):
    print 'Badly formatted challenge response: %s' % text
    return None
  return (splits[2], splits[4], splits[6], splits[8])



def challengeResponse(email, passwd, challenge):
  sha1 = hashlib.sha1()
  sha1.update("".join([challenge, passwd])) # hash the first elements
  digest = sha1.hexdigest()
  strAnswer = ''
  for i in range(0, len(digest)):
    strAnswer = strAnswer + digest[i]
  return strAnswer



def challenge_url():
  """Returns the challenge url."""
  return 'https://class.coursera.org/nlp/assignment/challenge'


def submit_url():
  """Returns the submission url."""
  return 'https://class.coursera.org/nlp/assignment/submit'

def submitSolution(email_address, ch_resp, part, output, source, state, ch_aux):
  """Submits a solution to the server. Returns (result, string)."""
  source_64_msg = email.message.Message()
  source_64_msg.set_payload(source)
  email.encoders.encode_base64(source_64_msg)

  output_64_msg = email.message.Message()
  output_64_msg.set_payload(output)
  email.encoders.encode_base64(output_64_msg)
  values = { 'assignment_part_sid' : ("%s-%d" % (homework_id(), part)), \
             'email_address' : email_address, \
             #'submission' : output, \
             'submission' : output_64_msg.get_payload(), \
             #'submission_aux' : source, \
             'submission_aux' : source_64_msg.get_payload(), \
             'challenge_response' : ch_resp, \
             'state' : state \
           }
  url = submit_url()
  data = urllib.urlencode(values)
  req = urllib2.Request(url, data)
  response = urllib2.urlopen(req)
  string = response.read().strip()
  # TODO parse string for success / failure
  result = 0
  return result, string

def source(partId):
  """Reads in the source files for a given partId."""
  src = ''
  src_files = sources()
  if partId <= len(src_files):
    flist = src_files[partId - 1]
    for fname in flist:
      # open the file, get all lines
      f = open(fname)
      src = src + f.read()
      f.close()
      src = src + '||||||||'
  return src

############ BEGIN ASSIGNMENT SPECIFIC CODE ##############

from Googling import GoogleQuery
from Googling import LocationPossibilities
from Googling import Googling
from Wiki import Wiki
import os

class NullDevice():
    def write(self, s):
        pass

def passXML():
    file = open('../XML/xml_output.txt')
    lines = file.readlines()
    line = ''.join(lines)
    return 'part[1]' + line

def scoreAnswers(guesses, gold, landmarks):
    correctCities = []
    incorrectCities = []
    noguessCities = []
    correctCountries = []
    incorrectCountries = []
    noguessCountries = []
    for i in range(len(guesses)):
        if guesses[i].city.lower() in gold[i].cities:
            correctCities.append(i)
        elif guesses[i].city == '':
            noguessCities.append(i)
        else:
            incorrectCities.append(i)
        if guesses[i].country.lower() == gold[i].country.lower():
            correctCountries.append(i)
        elif guesses[i].country == '':
            noguessCountries.append(i)
        else:
            incorrectCountries.append(i)
    correctGuesses = set(correctCities).intersection(set(correctCountries))
    noGuesses = set(noguessCities).union(set(noguessCountries))
    correntGuesses = set(incorrectCities).union(set(incorrectCountries))
    correctTotal = len(correctCities) + len(correctCountries)
    noguessTotal = len(noguessCities) + len(noguessCountries)
    incorrectTotal = len(incorrectCities) + len(incorrectCountries)
    return correctTotal - incorrectTotal

def constructDataObject(guesses):
    result = ''
    for guess in guesses:
        result += (guess.city + '##')
    result = result[:-2]
    result += '\n'
    for guess in guesses:
        result += (guess.country + '##')
    result = result[:-2]
    return result

def encodingGoogling(guesses):
    cities = []
    countries = []
    for guess in guesses:
        cities.append(guess.city)
        countries.append(guess.country)
    try:
        import json
        res_json = json.dumps([cities, countries])
    except ImportError:
        print '!!! Error importing json library. This is likely due to an early version of Python 2.6. Attempting to submit without json library. If this fails, please update to Python 2.7.'
        res_json = str([cities, countries])
        res_json = res_json.replace('\'', '\"')
    finally:
        return res_json


def gradeGoogling(partId, ch_aux):
    import sys
    original_stdout = sys.stdout #saves pointer to sys.stdout.
    sys.stdout = NullDevice() #blocks out print statements
    if partId == 3:
        googleResultsFile = '../data/googleResults_tagged.txt' # file where Google query results are read
        goldFile = '../data/landmarks.txt' # contains the results
    googling = Googling()
    if partId == 3:
        queryData = googling.readInData(googleResultsFile)
        goldData, landmarks = googling.readInGold(goldFile)
    if partId == 4:
        lines = ch_aux.split('\n')
        landmarkPart = lines[:10]
        queryPart = lines[11:]
        queryData = googling.readString(queryPart)
        landmarks = landmarkPart
    guesses = googling.processQueries(queryData)
    sys.stdout = original_stdout
    if partId == 3:
        return scoreAnswers(guesses, goldData, landmarks)
    else:
        return encodingGoogling(guesses)

def modifyWiki(ch_aux, wikiFile):
    file = open(wikiFile)
    lines = file.readlines()
    file.close()
    for i in range(len(lines)):
        lines[i] = lines[i].replace('Infobox', ch_aux)
        lines[i] = lines[i].replace('|spouse', ch_aux[3:])
    file = open('wiki_mod', 'w')
    file.writelines(lines)
    file.close()

def evaluateAnswers(husbandsLines, goldFile):
    import sys, traceback
    correct = 0
    wrong = 0
    noAnswers = 0
    score = 0
    try:
        goldData = open(goldFile)
        goldLines = goldData.readlines()
        goldData.close()

        goldLength = len(goldLines)
        husbandsLength = len(husbandsLines)

        if goldLength != husbandsLength:
            print('Number of lines in husbands file should be same as number of wives!')
            sys.exit(1)
        for i in range(goldLength):
            if husbandsLines[i].strip() in set(goldLines[i].strip().split('|')):
                correct += 1
                score += 1
            elif husbandsLines[i].strip() == 'No Answer':
                noAnswers += 1
            else:
                wrong += 1
                score -= 1
    except IOError:
        exc_type, exc_value, exc_traceback = sys.exc_info()
        traceback.print_tb(exc_traceback)
    finally:
        return score

def packageWiki(infoBoxHusbands, noInfoHusbands):
    result = ''
    for husband in infoBoxHusbands:
        result += (husband + '##')
    result = result[:-2]
    result += '\n'
    for husband in noInfoHusbands:
        result += (husband + '##')
    result = result[:-2]
    return result

def encodeWiki(infoBoxHusbands, noInfoHusbands):
    try:
        import json
        res_json = json.dumps([infoBoxHusbands, noInfoHusbands])
    except ImportError:
        print '!!! Error importing json library. This is likely due to an early version of Python 2.6. Attempting to submit without json library. If this fails, please update to Python 2.7.'
        res_json = str([infoBoxHusbands, noInfoHusbands])
        res_json = res_json.replace('\'', '\"')
    finally:
        return res_json

def gradeWiki(partId, ch_aux):
    import sys
    original_stdout = sys.stdout
    sys.stdout = NullDevice()
    wiki = Wiki()
    if partId == 1:
        wivesFile = '../data/wives.txt'
        goldFile = '../data/gold.txt'
        wives = wiki.addWives(wivesFile)
    else:
        wives = ch_aux.split('\n')
        wives = wives[1:]
    wikiFile = '../data/small-wiki.xml'
    infoBoxHusbands = wiki.processFile(open(wikiFile), wives, True)
    modifyWiki(ch_aux[0], wikiFile)
    mod_file = open('wiki_mod')
    noInfoHusbands = wiki.processFile(mod_file, wives, False)
    os.remove('wiki_mod')
    sys.stdout = original_stdout
    if partId == 1:
        infoScore = evaluateAnswers(infoBoxHusbands, goldFile)
        noInfoScore = evaluateAnswers(noInfoHusbands, goldFile)
        return (infoScore + noInfoScore)
    else:
        return encodeWiki(infoBoxHusbands, noInfoHusbands)

def output(partId, ch_aux):
    if partId == 1 or partId == 2:
        score = gradeWiki(partId, ch_aux)
        return 'part[' + str(partId) + ']' + '[' + str(score)
    if partId == 3 or partId == 4:
        score = gradeGoogling(partId, ch_aux)
        return 'part[' + str(partId) + ']' + '[' + str(score)
    else:
        print('Sorry, invalid part id')
submit(0)
