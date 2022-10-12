import collections
import csv
import re
import unidecode
import sys
import math

import xml.etree.cElementTree as et

# python3 -m pip install wikitextparser
import wikitextparser as wtp

# Use with commande : python3 read.py frwiki10000.xml

#################
# Configuration #
#################

MAX_PAGES = int(sys.argv[2])
path = sys.argv[1]
ixid_uri = 'http://www.mediawiki.org/xml/export-0.10/'
totalPagesRead = 0
totalWordsRead = 0

#Any words in this array will be searched for in the file
catchWords = ["math","algo","scienc","physi","biolog","histoi","techno"]

extractPath = path.split('.xml')[0]+"_extract.csv"
countPath = path.split('.xml')[0]+"_words.csv"

#Code stolen from stackoverflow to make reading csv work
maxInt = sys.maxsize

while True:
    try:
        csv.field_size_limit(maxInt)
        break
    except OverflowError:
        maxInt = int(maxInt/10)

################################
# Writer/Reader Initialisation #
################################

#Extract
extractWriter = open(extractPath,'w',newline='', encoding="utf8")
extractCsv_out = csv.writer(extractWriter)

#Log file
logWriter = open("../data/logs.txt",'w',newline='', encoding="utf8")

#Reader file
baseReader = open(path,encoding="utf8")

parser = et.iterparse(baseReader)

currentTitle = ""
currentText = ""

#############################################
# Step 1 : Title, text and links extraction #
#############################################

#Find every title and its text associated
for event, element in parser:

    #Save the most recent title and text seen
    if element.tag == et.QName(ixid_uri,'title'):
        currentTitle = element.text
        element.clear()

    if element.tag == et.QName(ixid_uri,'text'):
        currentText = element.text
        element.clear()
    

    if(currentText != "" and currentTitle != ""):

        if any(word in currentTitle for word in catchWords) or any(word in currentText for word in catchWords):#Catches the searched words

            try:
                #Clean the text of all bad formatings
                cleanedText = wtp.parse(currentText)
                
                #Extract and remove duplicates of links
                linkSet = set()
                linkString = "NoLinks"
                
                for x in cleanedText.wikilinks:
                    linkSet.add(x.title)
                    
                for x in linkSet:
                    if(linkString == "NoLinks"):
                        linkString = x+"\n"
                    else:
                        linkString += x+"\n"

                actualText = cleanedText.plain_text()#Obtain plain text 
                actualText = unidecode.unidecode(actualText).upper()
                actualText = re.sub(r'http\S+', '', actualText)#Remove urls
                actualText = re.sub(r'\b\w{1,3}\b', '', actualText)#Remove word of size < 3
                actualText = re.sub(r'[^0-9a-zA-Z \n]+', '', actualText)
                    
                    
                actualText = re.sub(' +', ' ', actualText)#Remove multiple spaces

                # Maybe remove line break altogether, jsp
                actualText = re.sub(r'\n\s*\n', '\n\n', actualText)#Remove multiple line breaks

                actualText = actualText.replace("\n ","\n")#Remove space after line break
                actualText = actualText.replace("\n ","\n")#Remove space after line break n2
                                            
                extractCsv_out.writerow((currentTitle,actualText,linkString))#Write in the csv

                if(len(sys.argv) > 3 and sys.argv[3] == "first"):
                    exit(0)

                if(totalPagesRead % 100 == 0):
                    print("Current page '"+currentTitle+"' count :",totalPagesRead,"out of",MAX_PAGES)

                totalPagesRead += 1 

            except Exception as e:
                print("Error on page ",totalPagesRead," : ",e)
                logWriter.write(str(e))

        if(totalPagesRead >= MAX_PAGES):#Custom that number
            print("Maximum size reached !")
            break
        
    currentText = ""


logWriter.close()
extractWriter.close()
baseReader.close()

print("Extract file created")

########################################
# Step 2 : Top 10 000 words extraction #
########################################

final = set()
toKeep = []
removed = []

occurences = collections.Counter()
uniqueOccurences = collections.Counter()

with open(extractPath, encoding="utf8") as f:

    datareader = csv.reader(f)

    count = 0

    for row in datareader:#Read each row of the file
        text = row[0]

        print("Currently processing page",count,"out of",totalPagesRead)

        count += 1

        unique = set()
        
        for i in range(2):#Read the first two columns of the csv : title,text,
            col = row[i]

            #Split into individual words
            
            words = re.split("\W+",col)

            #Update occurences
            for y in range(len(words)):

                if(words[y]) == '':#Ignore empty words
                    continue

                words[y] = unidecode.unidecode(words[y]).upper().strip()

                unique.add(words[y])#Keep track of unique words occurences in this page
                
                #Update the occurence count of each word in the list
                occurences[words[y]] += 1

                if(i == 0):#For each word of the title column
                    toKeep.append(words[y])#Keep it and add it to the 10k word list
            
        #Update unique occurences
        for y in unique:
            uniqueOccurences[y] += 1
                
    f.close()

    #Sort by occurence
    topWords = sorted(occurences.items(), key=lambda kv: kv[1], reverse=True)

    for w in toKeep:
        final.add((w,occurences[w],uniqueOccurences[w]))
        if(len(final) == 10000):
            break
        
    for w in topWords:
        final.add((w[0],w[1],uniqueOccurences[w[0]]))
        if(len(final) == 10000):
            break

    final = sorted(final, key=lambda kv: kv[1], reverse=True)

    #Clears thepath+ extract.csv file
    extractWriter = open(countPath,'w').close()

    extractWriter = open(countPath,'w',newline='', encoding="utf8")
    extractCsv_out = csv.writer(extractWriter)

    for val in final:
        totalWordsRead += 1
        extractCsv_out.writerow((val[0],val[2],math.log10(totalPagesRead/val[2])))#Word, total occurences, unique occurences, IDF
        
    print("Word file created")