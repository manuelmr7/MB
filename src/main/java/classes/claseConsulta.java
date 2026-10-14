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
    static final int FILAS = 10; //Puedo poner 100

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

    private static void lanza(SolrClient solr, PrintWriter out, String idConsulta, String texto)
            throws SolrServerException, IOException {

        String q = primerasPalabras(texto, NUM_PALABRAS);
        if (q.isEmpty()) return;

        SolrQuery query = new SolrQuery();
        query.setQuery("text:(" + q + ")");
        query.setFields("id", "score");
        query.setRows(FILAS);

        QueryResponse rsp = solr.query(query);
        for (SolrDocument doc : rsp.getResults()) {
            out.println(idConsulta + " " + doc.getFieldValue("id") + " " + doc.getFieldValue("score"));
        }
        System.out.println("Consulta " + idConsulta + " [" + q + "] -> "
                + rsp.getResults().getNumFound() + " resultados");
    }

    /** Devuelve las n primeras palabras, sin puntuación ni caracteres especiales de Solr. */
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