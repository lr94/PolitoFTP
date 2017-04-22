# PolitoFTP
PolitoFTP è un gateway che consente di accedere al materiale pubblicato sul Portale della Didattica del Politecnico di Torino mediante il protocollo FTP.

## Utilizzo
Una volta compilato, il programma va lanciato con

	java -jar politoftp.jar

A questo punto usando un qualsiasi client FTP (le prove sono state condotte con FileZilla) ci si può collegare all'indirizzo `127.0.0.1` sulla porta `5021` usando le proprie credenziali del Politecnico (sMATRICOLA e password).

## Problemi noti
* Ogni tanto il trasferimento di alcuni file non va a buon fine
* A  volte il server FTP smette di rispondere; non è un grosso problema perché il client FTP normalmente provvede a ricollegarsi autonomamente, ma questo rallenta notevolmente il tempo di download di cartelle intere.

## Note
* Al momento il server accetta connessioni in ingresso da qualsiasi indirizzo IP
* Non è ancora possibile modificare la porta del server

## Dipendenze
PolitoFTP necessita delle seguenti librerie:
* Apache HttpClient ( https://hc.apache.org/downloads.cgi )
* Apache Commons Lang ( https://commons.apache.org/proper/commons-lang/download_lang.cgi )
* JSON-Java ( https://github.com/stleary/JSON-java )
