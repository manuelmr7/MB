/*package classes;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

public class claseAdd {
    public static void main(String [] args) throws IOException, SolrServerException {
        String sCarpAct = System.getProperty("user.dir");
        String fileName = sCarpAct+"\\src\\corpus\\MED.ALL";
        Scanner scan = new Scanner(new File(fileName));
        String state="BEFORE";
        Long index = null,title=null,author=null,text=null;
        
        //Borremos colección
        //DeletingAllDocuments();
        
        while(scan.hasNextLine()){
            state="PROCESS";
            String line = scan.nextLine();
            System.out.println(line);
            if(line.startsWith(".I", 0)){  //indice del documento
                
                indexaDocumento(index, title, author, text);
                String[] parts = line.split(" ");
                index = parts[1];
                System.out.println(index);
            }
            if(line.startsWith(".W", 0)){  //titulo del documento     
                title=scan.nextLine();
            }
            if(state=="READING"){  //texto del documento 
                String part=scan.nextLine();
                if(part.startsWith(".X",0)){
                    state="IGNORING";
                }else{
                    text=text+part;
                }
            if((line.startsWith(".X", 0))||state=="IGNORING"){  //referencias cruzadas del documento     
                //no hago nada
            }else{
                //aquí tampoco
            }
        }
    }
        //last document
        indexaDocumento(index, title, author, text);
    }
    
    public static void indexaDocumento(Long index, String title,String author, String text) throws SolrServerException, IOException{
        HttpSolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/corpus").build();
		
		//Create solr document
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("index", index);
		doc.addField("title", title);
                doc.addField("author", author);
                doc.addField("text", text);
		solr.add(doc);
		solr.commit();
    }
    
    public static void DeletingAllDocuments () { 
   
      //Preparing the Solr client 
      String urlString = "http://localhost:8983/solr/micoleccion"; 
      SolrClient Solr = new HttpSolrClient.Builder(urlString).build();   
      
      //Preparing the Solr document 
      SolrInputDocument doc = new SolrInputDocument();   
          
        try {
            //Deleting the documents from Solr
            Solr.deleteByQuery("*");
            Solr.commit(); 
        } catch (SolrServerException ex) {
            Logger.getLogger(claseAdd.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(claseAdd.class.getName()).log(Level.SEVERE, null, ex);
        }
         
      //Saving the document 
      
      System.out.println("Documents deleted"); 
   } 
}*/
package classes;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

public class claseAdd {

    static final String URL = "http://localhost:8983/solr/corpus";

    public static void main(String[] args) throws IOException, SolrServerException {
        File file = new File(System.getProperty("user.dir"),
                "src" + File.separator + "corpus" + File.separator + "MED.ALL");

        try (SolrClient solr = new HttpSolrClient.Builder(URL).build();
             Scanner scan = new Scanner(file)) {
        	//deleteAllDocuments(solr);

            String id = null;
            StringBuilder text = new StringBuilder();
            boolean reading = false;
            int total = 0;

            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();

                if (line.startsWith(".I")) {
                    if (id != null) { indexa(solr, id, text.toString().trim()); total++; }
                    id = line.split("\\s+")[1];
                    text.setLength(0);
                    reading = false;
                } else if (line.startsWith(".W")) {
                    reading = true;
                } else if (reading) {
                    text.append(line).append(' ');
                }
            }
            if (id != null) { indexa(solr, id, text.toString().trim()); total++; }

            solr.commit();
            System.out.println("Documentos indexados: " + total); // debe salir 1033
        }
    }

    private static void indexa(SolrClient solr, String id, String text)
            throws SolrServerException, IOException {
        // Título = primera frase (hasta el primer " ."), como en MED
        int cut = text.indexOf(" .");
        String title = (cut > 0) ? text.substring(0, cut).trim() : text;

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("title", title);
        doc.addField("text", text);
        solr.add(doc);
    }

    public static void deleteAllDocuments(SolrClient solr) throws SolrServerException, IOException {
        solr.deleteByQuery("*:*");
        solr.commit();
    }
}