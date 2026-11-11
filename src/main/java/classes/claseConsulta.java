package classes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

public class claseConsulta {

    static final String URL = "http://localhost:8983/solr/corpus";
    static final int FILAS = 100;
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

    // Parámetros eDisMax
    static final String QF  = "title^3 text^1"; // Mayor prioridad a términos coincidentes en título
    static final String PF  = "title^3 text^1.5"; // Bonus por frases exactas
    static final String PS  = "2"; // Tolerancia estricta de distancia entre términos de frase
    static final String MM  = "15%"; // Coincidencia mínima requerida
    static final String TIE = "0.1";

    private static void lanza(SolrClient solr, PrintWriter out, String idConsulta, String texto)
            throws SolrServerException, IOException {

        String q = limpiarConsulta(texto);
        if (q.isEmpty()) return;

        SolrQuery query = new SolrQuery(q);
        query.set("defType", "edismax");
        query.set("qf", QF);
        query.set("pf", PF);
        query.set("ps", PS);
        query.set("mm", MM);
        query.set("tie", TIE);
        query.setFields("id", "score");
        query.setRows(FILAS);

        QueryResponse rsp = solr.query(query);
        int ranking = 0;
        for (SolrDocument doc : rsp.getResults()) {
            out.println(idConsulta + " Q0 " + doc.getFieldValue("id") + " " + ranking + " "
                    + doc.getFieldValue("score") + " " + NOM);
            ranking++;
        }
    }

    /** Limpia signos de puntuación y normaliza espacios. Solr se encarga de filtrar las stopwords. */
    private static String limpiarConsulta(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
    }
}