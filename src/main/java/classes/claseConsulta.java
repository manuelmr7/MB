package classes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

public class claseConsulta {

    static final String URL = "http://localhost:8983/solr/corpus";
    static final int NUM_PALABRAS = 5;
    static final int FILAS = 100; //Puedo poner 100
    static final String NOM = "Manuel";

    public static void main(String[] args) throws IOException, SolrServerException {
        File qry = new File(System.getProperty("user.dir"),
                "src" + File.separator + "corpus" + File.separator + "MED.QRY");
        File salida = new File(System.getProperty("user.dir"), "resultados.txt");

        try (SolrClient solr = new HttpSolrClient.Builder(URL).build();
             Scanner scan = new Scanner(qry);
             PrintWriter out = new PrintWriter(salida)) {

            String id = null;
            StringBuilder texto = new StringBuilder();
            boolean reading = false;

            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();

                if (line.startsWith(".I")) {
                    if (id != null) lanza(solr, out, id, texto.toString());
                    id = line.split("\\s+")[1];
                    texto.setLength(0);
                    reading = false;
                } else if (line.startsWith(".W")) {
                    reading = true;
                } else if (reading) {
                    texto.append(line).append(' ');
                }
            }
            if (id != null) lanza(solr, out, id, texto.toString());
        }
        System.out.println("Resultados guardados en " + salida.getAbsolutePath());
    }

    // Parámetros de eDisMax
    static final String QF   = "title^2 text^1"; //en qué campos busca y cuánto pesa cada uno
    static final String PF   = "title^4 text^2"; //bonus extra cuando las palabras de la consulta aparecen juntas y en orden en el documento
    static final String PF2  = "text^2";
    static final String PF3  = "text^1";
    static final String PS   = "3"; //cuánto pueden estar separadas esas palabras para seguir contando como frase.
    static final String MM   = "30%"; //porcentaje mínimo de palabras de la consulta que debe tener un documento para salir.
    static final String TIE  = "0.3"; //cuánto suma que la palabra aparezca también en el segundo campo, además del mejor.

    private static void lanza(SolrClient solr, PrintWriter out, String idConsulta, String texto)
            throws SolrServerException, IOException {

        String q = primerasPalabras(texto, NUM_PALABRAS);
        if (q.isEmpty()) return;

        SolrQuery query = new SolrQuery(q);          // sin "text:(...)": lo gestiona qf
        query.set("defType", "edismax");
        query.set("qf", QF);
        query.set("pf", PF);
        query.set("pf2", PF2);
        query.set("pf3", PF3);
        query.set("ps", PS);
        query.set("mm", MM);
        query.set("tie", TIE);
        query.set("lowercaseOperators", "false");    // "and"/"or" como palabras normales
        query.set("stopwords", "true");
        query.setFields("id", "score");
        query.setRows(FILAS);

        QueryResponse rsp = solr.query(query);
        int ranking = 0;
        for (SolrDocument doc : rsp.getResults()) {
            out.println(idConsulta + " Q0 " + doc.getFieldValue("id") + " " + ranking + " "
                    + doc.getFieldValue("score") + " " + NOM);
            ranking++;
        }
        System.out.println("Consulta " + idConsulta + " -> "
                + rsp.getResults().getNumFound() + " resultados");
    }
    

    /** Devuelve las n primeras palabras (variable NUM_PALABRAS), sin puntuación ni caracteres especiales de Solr. */
    private static String primerasPalabras(String texto, int n) {
        List<String> palabras = new ArrayList<>();
        for (String w : texto.trim().split("\\s+")) {
            String limpia = w.replaceAll("[^\\p{L}\\p{N}]", "");
            if (!limpia.isEmpty()) palabras.add(limpia);
            if (palabras.size() == n) break;
        }
        return String.join(" ", palabras);
    }
}