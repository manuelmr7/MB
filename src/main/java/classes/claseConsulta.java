package classes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

public class claseConsulta {

    static final String URL = "http://localhost:8983/solr/corpus";
    static final int FILAS = 100; // Evaluaciones TREC requieren recuperar hasta 100 docs
    static final String NOM = "Manuel";

    // Conjunto de palabras vacías (Stop Words) en inglés para limpiar la consulta en Java
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
        "between", "both", "but", "by", "can", "could", "did", "do", "does", "doing",
        "down", "during", "each", "few", "for", "from", "further", "had", "has", "have",
        "having", "he", "her", "here", "hers", "how", "i", "if", "in", "into", "is",
        "it", "its", "me", "more", "most", "my", "no", "nor", "not", "of", "off", "on",
        "once", "only", "or", "other", "our", "out", "over", "own", "same", "she",
        "should", "so", "some", "such", "than", "that", "the", "their", "them", "then",
        "there", "these", "they", "this", "those", "through", "to", "too", "under",
        "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "whom", "why", "with", "would", "you", "your", "results",
        "cases", "report", "reported", "describe", "described", "patient", "patients"
    ));

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

    // Parámetros eDisMax reajustados para consultas médicas limpias
    static final String QF  = "title^3 text^1"; // Mayor prioridad a términos coincidentes en título
    static final String PF  = "title^3 text^1.5"; // Bonus por frases exactas sin stop words
    static final String PS  = "2"; // Tolerancia estricta de distancia de frase
    static final String MM  = "15%"; // MM bajo para no penalizar consultas con muchos términos técnicos
    static final String TIE = "0.1";

    private static void lanza(SolrClient solr, PrintWriter out, String idConsulta, String texto)
            throws SolrServerException, IOException {

        String q = filtrarYLimpiarConsulta(texto);
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

    /** Elimina puntuación y filtra stop words, dejando solo los términos sustantivos de la consulta. */
    private static String filtrarYLimpiarConsulta(String texto) {
        if (texto == null) return "";
        String[] tokens = texto.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        List<String> validTokens = new ArrayList<>();

        for (String t : tokens) {
            if (t.length() > 1 && !STOP_WORDS.contains(t)) {
                validTokens.add(t);
            }
        }
        return String.join(" ", validTokens);
    }
}