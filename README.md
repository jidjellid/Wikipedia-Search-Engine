Ouvrir un terminal dans le dossier "src"

Utilisation :

    1ere Etape : read.py

        Depuis le dossier "src" :

            Extraction des pages et des 10 000 mots

                read.py monFichier.xml nbPages

            Exemple:

                read.py ../data/frwiki-latest-pages-articles.xml 20000

        Si le nombre de donné est superieur ou égal aux nombre réel de pages, toutes les pages du fichier donné seront lues

    2eme Etape : Main.java

        Le chargement en mémoire des pages consomme environ 4Go de ram. En cas de problème lors du lancement, il est conseillé de rajouter l'option -Xmx6g

        Depuis le dossier "src" :

            Compilation  : 

                Windows : javac -cp "univocity-parsers-2.9.2.jar;" Main.java
                Unix    : javac -cp "univocity-parsers-2.9.2.jar:" Main.java

            Execution : 

                java Main racinePath nbMots nbPages

            Exemple :

                java Main E:/ProjectPath/data/200k
                java -Xmx6g Main ../data/200k

    Mode serveur : 
        
        Le programme peut tourner en boucle et réceptionner la création de fichiers "request.txt" en précisant le chemin du dossier contenant ces fichiers.
        Dans ce cas là, il produira une réponse sous la forme d'un fichier "response.txt" contenant le résultat de la recherche.

        Depuis le dossier "src" :

            Exemple : 

                java Main E:/ProjectPath/data/200k E:/ProjectPath/server
                java -Xmx6g Main ../data/200k ../server
        
        Il faudra penser le serveur dans un autre terminal avec la commande suivante :

            Depuis le dossier "server" : 

                node server.js

        