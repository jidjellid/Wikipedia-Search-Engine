import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.text.Normalizer;

import com.univocity.parsers.csv.*;

public class Relations {

    int nbMots, nbPages;
    Mot[] mots;
    
    //Matrix
    MatriceCLI liensCLI;

    //Page list
    float [] pageRank;
    double [] pageNorm;
    
    HashMap<String, Integer> convertWord2idx = new HashMap<>();//Singular word to index

    HashMap<String, Integer> convertStr2pageIdx = new HashMap<>();//String to page index
    HashMap<Integer, String> convertPageIdx2Str = new HashMap<>();//Reverse xd

    public class Mot {
        HashMap<Integer, Integer> page2idx = new HashMap<>();//Takes a page number and returns its index in pages

        String value;
        double IDF;
        int position;

        Page [] pages;//Array of the indexes of the pages the word appears in

        class Page{

            int index;
            int occurences;//Array of the occurences of the page in "pages" at index i
            double TF;
            double normTF;
            double score;

            Page(int pageNumber){
                index = pageNumber;
            }
        }
        
        Mot(String value, int nbPagesWhereWordAppears) {
            this.value = value;
            this.pages = new Page[nbPagesWhereWordAppears];
        }

        //Add a page, updating the necessary values
        void addPage(int pageNumber){
            if(page2idx.get(pageNumber) == null){//If the page has not yet been added to this word
                page2idx.put(pageNumber, position);
                pages[position] = new Page(pageNumber);
                pages[position].occurences++;
                pages[position].TF = 1 + Math.log10(pages[position].occurences);
                position++;
            } else {
                int pagePosition = page2idx.get(pageNumber);
                pages[pagePosition].occurences++;
                pages[pagePosition].TF = 1 + Math.log10(pages[pagePosition].occurences);
            }
        }
    }

    public static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    public void saveData(String pathFolder){
        try{
            //CLI
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFolder+"/CLI.txt"), StandardCharsets.UTF_8));

            writer.write(nbMots+" "+nbPages+"\n");
            for(float c : liensCLI.C){
                writer.write(c+" ");
            }
            writer.write("\n");

            for(int l : liensCLI.L){
                writer.write(l+" ");
            }
            writer.write("\n");

            for(int i : liensCLI.I){
                writer.write(i+" ");
            }               
            writer.close();
            
            //Pages 
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFolder+"/Pages.txt"), StandardCharsets.UTF_8));

            for(int i = 0; i < nbPages; i++){
                writer.write(convertPageIdx2Str.get(i)+";"+pageRank[i]+";"+pageNorm[i]+";"+"\n");//Every line : pageRank pageNorm title
            }
                        
            writer.close();

            //Relations
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFolder+"/Relations.txt"), StandardCharsets.UTF_8));

            for(int i = 0; i < mots.length; i++){
                Mot current = mots[i];
                writer.write(current.value+";"+current.IDF+";"+current.position+";");//Start of line : value score IDF position
                for(int y = 0; y < current.position; y++){
                    writer.write(current.pages[y].index+";"+current.pages[y].occurences+";"+current.pages[y].TF+";"+current.pages[y].normTF+";"+current.pages[y].score+";");//Rest of line : page1index page1occurences page1TF page1normTF page2index page2occurences page2TF page2normTF
                }
                writer.write("\n");
            }
                        
            writer.close();

        } catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void loadData(String path){
        try{

            //CLI Ok
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path+"/CLI.txt"), StandardCharsets.UTF_8));

            String [] splitted = reader.readLine().split(" ");
            nbMots = Integer.valueOf(splitted[0]);
            nbPages = Integer.valueOf(splitted[1]);

            splitted = reader.readLine().split(" ");
            float [] C = new float[splitted.length];
            for(int i = 0; i < splitted.length; i++){
                C[i] = Float.valueOf(splitted[i]);
            }

            splitted = reader.readLine().split(" ");
            int [] L = new int[splitted.length];
            for(int i = 0; i < splitted.length; i++){
                L[i] = Integer.valueOf(splitted[i]);
            }

            splitted = reader.readLine().split(" ");
            int [] I = new int[splitted.length];
            for(int i = 0; i < splitted.length; i++){
                I[i] = Integer.valueOf(splitted[i]);
            }

            reader.close();

            liensCLI = new MatriceCLI(C, L, I);
            pageNorm = new double[nbPages];
            pageRank = new float[nbPages];
            mots = new Mot[nbMots];

            //Pages
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path+"/Pages.txt"), StandardCharsets.UTF_8));

            String line;
            int count = 0;
            while((line = reader.readLine()) != null) {
                splitted = line.split(";");
                convertStr2pageIdx.put(splitted[0], count);
                convertPageIdx2Str.put(count, splitted[0]);
                pageRank[count] = Float.valueOf(splitted[1]);
                pageNorm[count] = Double.valueOf(splitted[2]);
                count++;
            }
                     
            reader.close();

            //Relations
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path+"/Relations.txt"), StandardCharsets.UTF_8));

            count = 0;
            while((line = reader.readLine()) != null) {
                splitted = line.split(";");
                mots[count] = new Mot(splitted[0], Integer.valueOf(splitted[2]));//Create the word with the number of apparition given in the IDF file
                mots[count].IDF = Double.valueOf(splitted[1]);
                mots[count].position = Integer.valueOf(splitted[2]);
                mots[count].pages = new Mot.Page [mots[count].position];
                convertWord2idx.put(splitted[0], count);

                int subCount = 0;
                for(int i = 3; subCount < mots[count].position; i += 5){
                    mots[count].pages[subCount] = mots[count].new Page(Integer.valueOf(splitted[i])); 
                    mots[count].pages[subCount].occurences = Integer.valueOf(splitted[i+1]);
                    mots[count].pages[subCount].TF = Double.valueOf(splitted[i+2]);
                    mots[count].pages[subCount].normTF = Double.valueOf(splitted[i+3]);
                    mots[count].pages[subCount].score = Double.valueOf(splitted[i+4]);
                    mots[count].page2idx.put(Integer.valueOf(splitted[i]),subCount);
                    subCount++;
                }
                
                count++;
            }
                     
            reader.close();
        } catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }
    

    //Search pageText for any of the 10k words saved and add to any match the pageIndex given
    public void addRelations(String pageText, int pageIndex){
        String[] words = pageText.split(" ");

        for (int i = 0; i < words.length; i++) {
            //If the word found is one of the 10 000 saved
            if (convertWord2idx.get(words[i]) != null) {
                int idxMot = convertWord2idx.get(words[i]);
                Mot wordToAdd = mots[idxMot];
                
                //Update the "pages" property of the Mot found
                try{
                    wordToAdd.addPage(pageIndex);  
                } catch (Exception e){
                    System.out.println("Failed to add page "+convertPageIdx2Str.get(pageIndex)+" to word '"+words[i]+"' at position "+wordToAdd.position+"\nOther pages in this word are :");

                    for(Mot.Page p : wordToAdd.pages){
                        
                        System.out.print(convertPageIdx2Str.get(p.index)+", ");
                    }

                    e.printStackTrace();
                    
                    System.exit(0);
                }                
            }
        }
    }

    public static String[] cleanRequest(String val){
        Set<String> set = new HashSet<String>();

        for(String s : val.split(" ")){
            String stripped = Relations.stripAccents(s.toUpperCase());
            set.add(stripped);
        }

        return set.toArray(new String[set.size()]);
    }

    public String[][] simpleSearch(String request){
        Set<Mot.Page> validList = new HashSet<Mot.Page>();

        //Get the index list from the request
        String [] requestToArray = cleanRequest(request);
        int [] idxRequest = new int[requestToArray.length];
        for(int i = 0; i < requestToArray.length; i++){
            if(convertWord2idx.get(requestToArray[i]) == null){
                return new String[0][3];
            }
            idxRequest[i] = convertWord2idx.get(requestToArray[i]);
        }

        //For the first word, add all of the pages that contain it
        Mot firstWord = mots[idxRequest[0]];
        for(int i = 0; i < firstWord.position; i++){
            if(firstWord.pages[i].score > 0.01){//Check if the IDF(word) * TF(word,page) / N(page) is high enough
                validList.add(firstWord.pages[i]);
            }
        }

        //Check the remaining words in the request and delete any page that don't contain the currently parsed word
        for(int i = 1; i < idxRequest.length; i++){
            Mot currentWord = mots[idxRequest[i]];
            Iterator<Mot.Page> validListIterator = validList.iterator();

            while(validListIterator.hasNext()){
                Mot.Page val = validListIterator.next();
                if(currentWord.page2idx.get(val.index) == null || currentWord.pages[currentWord.page2idx.get(val.index)].score <= 0.01){//If the word does not appear in page y of the validList
                    validListIterator.remove();//Remove it
                }
            }
        }

        double alpha = 1/Math.sqrt(nbPages);
        double beta = 1 - alpha;

        //ArrayList to array
        Mot.Page [] pageArray = validList.toArray(new Mot.Page[0]);

        //For each page, find the score by 
        double [] fScores = new double[pageArray.length];

        //Square root of the sum of the IDF squared of each word in the request (CF TP3)
        double requestNorm = 0;
        for(int i = 0; i < idxRequest.length; i++){
            Mot requestWord = mots[idxRequest[i]];
            requestNorm += Math.pow(requestWord.IDF,2);
        }
        requestNorm = Math.sqrt(requestNorm);

        //Final score of each page, do some strange voodoo math
        for(int i = 0; i < pageArray.length; i++){
            Mot.Page page = pageArray[i];

            //Compute TF-IDF
            double TFIDF = 0;
            for(int y = 0; y < idxRequest.length; y++){//For each page of the request
                int requestWordIndex = idxRequest[y];
                Mot requestWord = mots[requestWordIndex];
                TFIDF += requestWord.IDF * requestWord.pages[requestWord.page2idx.get(page.index)].TF;
            }
  
            fScores[i] = alpha * (TFIDF/(pageNorm[page.index] * requestNorm)) + beta * (pageRank[page.index]);
        }
        class Capsule{
            Mot.Page page;
            Double score;

            Capsule(Mot.Page i, double s){
                page = i;
                score = s;
            }        
        }

        //Encapsulate to sort
        Capsule [] encapsuled = new Capsule[pageArray.length];
        for(int i=0; i < pageArray.length; i++){
            encapsuled[i] = new Capsule(pageArray[i],fScores[i]);
        }

        Arrays.sort(encapsuled, (a,b) -> a.score.compareTo(b.score));
       
        int [] extracted = new int[encapsuled.length];
        Capsule [] reversed = new Capsule[pageArray.length];

        int pos = 0;
        for(int i = encapsuled.length-1; i >= 0; i--){
            extracted[pos] = encapsuled[i].page.index;
            reversed[pos++] = encapsuled[i];
        }

        int nbValuesReturned = Math.min(50,reversed.length);
        String [][] finalForm = new String[nbValuesReturned][3];

        for(int i = 0; i < finalForm.length; i++){
            finalForm[i][0] = convertPageIdx2Str.get(reversed[i].page.index);
            finalForm[i][1] = "https://fr.wikipedia.org/wiki/"+convertPageIdx2Str.get(reversed[i].page.index).replace(" ", "_");
            finalForm[i][2] = reversed[i].score.toString();
        }

        return finalForm;
    }

    public void computeFromCSV(String filename) {
        try {
            CsvParserSettings settings = new CsvParserSettings();
            settings.setMaxCharsPerColumn(100000000);// Add a 0 if you get an array out of bound error during parsing
            settings.getFormat().setLineSeparator("\n");
            CsvParser parser = new CsvParser(settings);

            String[] row;

            int count;
            BufferedReader reader;

            /********************************************************
            Step 1 :
            Compute page count and add page title to hashmaps
            *********************************************************/
            System.out.println("\nStep 1 : Finding titles, page and link counts");
            
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename + "_extract.csv")), "UTF-8"));
            parser.beginParsing(reader);
            count = 0;

            while ((row = parser.parseNext()) != null && (count < nbPages || nbPages == 0)) {
                if (count % 1000 == 0 && count != 0) {
                    System.out.println("Total pages count : " + count);
                }

                //Associate the title string to the page index
                convertStr2pageIdx.put(row[0], count);
                convertPageIdx2Str.put(count, row[0]);

                count++;
            }

            //Correct maximum number of pages if too big
            if(count < nbPages || nbPages == 0)
                nbPages = count;

            //Initialise arrays with size, this place is safer than in the constructor as we check sizes here
            pageNorm = new double[nbPages];

            reader.close();
            parser.stopParsing();

            /********************************************************
            Step 2 :
            Compute the number of links that lead to pages title we have stored
            *********************************************************/
            System.out.println("\nStep 2 : Finding internal link count");

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename + "_extract.csv")), "UTF-8"));
            parser.beginParsing(reader);
            
            int validLinks = 0;
            int [] validLinks2 = new int[nbPages];
            count = 0;
            while ((row = parser.parseNext()) != null && count < nbPages) {
                if (count % 1000 == 0 && count != 0) {
                    System.out.println("Total pages count : " + count);
                }

                if(row[2] != null && !row[2].equals("NoLinks")){
                    String [] liensPage = row[2].strip().split("\n");
                    for(int i = 0; i < liensPage.length; i++)
                        liensPage[i] = liensPage[i].strip();
    
                    for(int i = 0; i < liensPage.length; i++){
                        if(convertStr2pageIdx.get(liensPage[i]) != null){
                            validLinks++;
                            validLinks2[count]++;
                        }
                    }
                }
               
                count++;
            }

            reader.close();
            parser.stopParsing();

            System.out.println("Final page count : " + count);

            liensCLI = new MatriceCLI(validLinks,nbPages+1,validLinks);

            /********************************************************
            Step 3.1 :
            Count the number of words in "words"
            *********************************************************/
            System.out.println("\nStep 3.1 : Couting number of words");
            
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename + "_words.csv")), "UTF-8"));
            
            parser.beginParsing(reader);

            count = 0;
            while ((row = parser.parseNext()) != null) {            
                count++;
            }

            nbMots = count;
            mots = new Mot[nbMots];

            reader.close();
            parser.stopParsing();

            /********************************************************
            Step 3.2 :
            Put the 10k top words into dictionaries and initialize them
            *********************************************************/
            System.out.println("\nStep 3 : Initialising top 10k words");
            
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename + "_words.csv")), "UTF-8"));
            
            parser.beginParsing(reader);

            count = 0;
            while ((row = parser.parseNext()) != null) {
                convertWord2idx.put(row[0], count);

                try{
                    mots[count] = new Mot(row[0], Integer.valueOf(row[1]));//Create the word with the number of apparition given in the IDF file
                    mots[count].IDF = Double.valueOf(row[2]);
                } catch (Exception e){
                    e.printStackTrace();
                    System.exit(0);
                }
                
                count++;
            }

            reader.close();
            parser.stopParsing();

            //Ideally, this doesn't do anything, but if the number of words given in argument is lower than the real amount, correct it
            if(nbMots > count){
                System.out.println("WARNING : Le nombre de mots passes en argument ("+nbMots+") est plus grand que le nombre de mots parses ("+count+")");
                nbMots = count;
            }
            
            /********************************************************
            Step 4 :
            Parse the text to fill the word-page relations and add links to the CLI
            *********************************************************/
            System.out.println("\nStep 4 : Filling word/page relations and adding links");

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename + "_extract.csv")), "UTF-8"));
            parser.beginParsing(reader);
            
            count = 0;

            //Parse every page
            while ((row = parser.parseNext()) != null && count < nbPages) {
                if (count % 1000 == 0 && count != 0) {
                    System.out.println("Currently at " + count + " out of " + nbPages);
                }

                
                if(row[1] != null){
                    //addRelations(row[0], count);
                    addRelations(row[1], count);//Add word to page relations for this page pages text

                    //Internal links update
                    if(row[2] != null && !row[2].equals("NoLinks")){
                        String[] liensPage = row[2].split("\n");
                        for(int i = 0; i < liensPage.length; i++)
                            liensPage[i] = liensPage[i].strip();

                        liensCLI.L[liensCLI.posL+1] = liensCLI.L[liensCLI.posL] + liensPage.length;

                        for(int i = 0; i < liensPage.length; i++) {
                            Integer strIdx = convertStr2pageIdx.get(liensPage[i]);

                            if(strIdx != null) {
                                liensCLI.C[liensCLI.posCI] = (float) (1./(float)validLinks2[count]);
                                liensCLI.I[liensCLI.posCI++] = strIdx;
                            } else {
                                liensCLI.L[liensCLI.posL+1]--;
                            }
                        }
                    }
                }

                liensCLI.posL++; 
                count++;
            }
            
            reader.close();
            parser.stopParsing();

            /********************************************************
            Step 5 :
            Computation of the norm of each page
            *********************************************************/
            System.out.println("\nStep 5 : Computing of the norm of each page");

            count = 0;
            for(int i = 0; i < nbPages; i++){
                if (count % 1000 == 0 && count != 0) {
                    System.out.println("Currently at " + count + " out of " + nbPages);
                }

                //For each page, compute the norm based on the TF of each word (in the 10k list) of that page
                for(int y = 0; y < nbMots; y++){
                    
                    Mot currentWord = mots[y];

                    if(currentWord == null){
                        System.out.println("Erreur : le mot a l'index "+y+" est null sur un total de " + nbMots + ", le fichier contient bien le nombre de lignes annonces ?");
                        System.exit(0);
                    }

                    if(currentWord.page2idx.get(i) != null){//Check if the page contains the current word
                        int posOfPageInWord = currentWord.page2idx.get(i);

                        pageNorm[i] += Math.pow(currentWord.pages[posOfPageInWord].TF, 2);
                    }
                }
                
                pageNorm[i] = Math.sqrt(pageNorm[i]);
                count++;
            }

            
            /********************************************************
            Step 6 :
            Once the norm for each page has been computed, compute the normalized TF each word-page
            *********************************************************/
            System.out.println("\nStep 6 : Computing normalized TF of each word-page");

            for(int i = 0; i < nbMots; i++){
                Mot currentWord = mots[i];

                for(int y = 0; y < currentWord.position; y++){
                    //For each word and page, compute the normalized TF
                    currentWord.pages[y].normTF = currentWord.pages[y].TF / pageNorm[currentWord.pages[y].index];

                    //Keep only the pages with a good enough score
                    currentWord.pages[y].score = currentWord.IDF * currentWord.pages[y].normTF;
                }
            }

            /********************************************************
            Step 7 :
            Find pageRank score for every page
            *********************************************************/

            System.out.println("\nStep 7 : Computing PageRank");

            MatriceCLI A = liensCLI;
            SquaredMatriceCLI J  = new SquaredMatriceCLI(A.L.length-1);

            float epsilon = 0.15f;
            MatriceCLI AG = MatriceCLI.matrice_AG(A, J, epsilon);

            float[] distribution = new float[liensCLI.L.length - 1];
            for(int y = 0; y < distribution.length; y++) {
                distribution[y] = (1f/distribution.length);
            }
            pageRank = A.pageRank(AG, J, distribution, 50, epsilon);  
      
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Current folder : "+System.getProperty("user.dir"));
            System.exit(0);
        }
    }
}
