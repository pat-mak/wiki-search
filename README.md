=== Projekt na warsztaty lucene ===

Po pierwsze musimy się przyznać, że konieczna będzie jeszcze wiedza dotycząca mavena i gita. Na szczęście wystarczą podstawy. 
W projekcie znajdziecie tag o nazwie starting-point. Byłoby świetnie jeśli udałoby się wam uruchomić ten projekt u siebie lokalnie. Potrzebny będzie git, maven i jakiś serwer z obsługą JSP. 

Jeśli wiesz jak to zrobić to dalej już nie czytaj ;)

Poniżej dokładne wskazówki (dlatego tak ich dużo ;)) jak to zrobić.

Na warsztatach będziemy używali eclipse'a i TomEE'ego. Na stronie TomEE'ego jest filmik jak ściągnąć eclipse'a i podpiąć pod niego TomEE'ego: http://tomee.apache.org/apache-tomee.html

Eclipse w wersji, której będziemy używać na zajęciach (http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/juno/SR2/eclipse-jee-juno-SR2-linux-gtk.tar.gz - wersja na linuxa) posiada domyślnie wsparcie dla GITa.

Następnie zaciągamy projekt np. tak:
git clone https://github.com/pat-mak/tjug-wiki-search.git
i przełączamy się na tag starting-point:

git checkout starting-point

Potem w eclipse wybieramy File->Import...->Git>Projects from Git. Następnie wybieramy, żeby użył "wizarda" i wybieramy Web->Dynamic Web Project. 
Po zaimportowaniu pozostaje jeszcze kwestia mavena. W Help->Install New Software  wybieramy All Available Sites i wpisujemy do wyszukiwania maven i czekamy spokojnie, bo eclipse długo myśli, ale powinien znaleźć coś co nazywa się m2e. Instalujemy to cudo i teraz jak klikniemy prawym na projekt to możemy wybrać Configure->Convert to Maven Project - czerowny X powinien zniknąć :) 
Teraz podpinamy projekt pod TomEE'ego i uruchamiamy serwer. Na stronie index.jsp powinna pojawić się 500, bo jar z lucene nie trafił na serwer. We właściwościach projektu wybieramy Deployment Assembly->Add..->Java Build Path Entries->Maven Dependencies . Teraz, po synchronizacji powinno już banglać.


=== Dodatkowe info ===
Plik do indeksowania można pobrać tu: http://dumps.wikimedia.org/plwiki/20130502/plwiki-20130502-pages-articles.xml.bz2
