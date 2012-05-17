import sys, traceback
import re

class Wiki:
    
    # reads in the list of wives
    def addWives(self, wivesFile):
        try:
            input = open(wivesFile)
            wives = input.readlines()
            input.close()
        except IOError:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            traceback.print_tb(exc_traceback)
            sys.exit(1)    
        return wives
    
    # read through the wikipedia file and attempts to extract the matching husbands. note that you will need to provide
    # two different implementations based upon the useInfoBox flag. 
    def processFile(self, f, wives, useInfoBox):
        
        husbands = []

        if useInfoBox:
            wives_to_husbands = self.parseWivesFromInfoBox(f)
        else:
            wives_to_husbands = self.parseWivesFromRawText(f)
        
        # print wives_to_husbands
        # print wives

        for wife in wives:
            if wife.strip() in wives_to_husbands:
                husbands.append("Who is " + wives_to_husbands[wife.strip()] + "?")
            else:
                husbands.append('No Answer')
        f.close()
        return husbands

    def parseWivesFromInfoBox(self, f):
        infoboxes = self.parseInfoBoxes(f)
        wives_to_husbands = {}
        for infobox in infoboxes:
            spouse = self.parseFromInfoBox("spouse", infobox)
            if not spouse:
                spouse = self.parseSpouseFromInfoBox(infobox)
            name = self.parseFromInfoBox("name", infobox)
            if spouse and name:
                wives_to_husbands[spouse] = name

        return self.addTruncatedSurnames(wives_to_husbands)

    def addTruncatedSurnames(self, wives_to_husbands):
        extras = {}

        for wife in wives_to_husbands:
            shusband = wives_to_husbands[wife].split(" ")
            swife = wife.split(" ")
            no_surname_wife = []
            for term in swife:
                if term not in shusband and "&quot" not in term:
                    no_surname_wife.append(term)

            extras[" ".join(no_surname_wife)] = wives_to_husbands[wife]

        wives_to_husbands.update(extras)
        return wives_to_husbands

    def parseWivesFromRawText(self, f):
        # parse page from file
        # search terms like [married, who married]
        # search [[Phrases]] within 3 word difference
        # compare **
        # search Capitalized Names within 5 word difference
        # compare **

        # compare = search on the other side (on the left prepositions - on the right prep's)
        return {}

    def parseSpouseFromInfoBox(self, infobox):
        pattern = re.compile("\[\[((?:\w+\.?\s)*\w+)\]\]")
        for line in infobox:
            if "spouse" in line.lower():
                res = pattern.search(line)
                if res:
                    return res.group(1)

        return None

    def parseInfoBoxes(self, f):
        infoboxes = []

        lines = f.readlines()
        read_to_buffer = False
        infobox = []

        for line in lines:
            if "{{Infobox" in line:
                read_to_buffer = True
            elif line.startswith("}}"):
                read_to_buffer = False
                infoboxes.append(infobox)
                infobox = []
            
            if read_to_buffer:
                infobox.append(line)

        return infoboxes

    def parseFromInfoBox(self, term, infobox):
        search_pattern = "\|\s*" + self.enhanceTerm(term) + "\s*=\s?\[?\[?((?:(?:&quot;)?\w+\.?(?:&quot;)?\s)*\w+)"
        pattern = re.compile(search_pattern)
        for line in infobox:
            res = pattern.match(line)
            if res:
                return res.group(1)

        return None

    def enhanceTerm(self, term):
        return "(?:{0}|{1}|{2})".format(term.lower(), term.upper(), term[0].upper() + term[1:].lower())

    
    # scores the results based upon the aforementioned criteria
    def evaluateAnswers(self, useInfoBox, husbandsLines, goldFile):
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
                # print husbandsLines[i], " ----- ", goldLines[i]
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
        if useInfoBox:
            print('Using Info Box...')
        else:
            print('No Info Box...')
        print('Correct Answers: ' + str(correct))
        print('No Answers: ' + str(noAnswers))
        print('Wrong Answers: ' + str(wrong))
        print('Total Score: ' + str(score)) 

if __name__ == '__main__':
    wikiFile = '../data/small-wiki.xml'
    wivesFile = '../data/wives.txt'
    goldFile = '../data/gold.txt'
    useInfoBox = True;
    wiki = Wiki()
    wives = wiki.addWives(wivesFile)
    husbands = wiki.processFile(open(wikiFile), wives, useInfoBox)
    wiki.evaluateAnswers(useInfoBox, husbands, goldFile)