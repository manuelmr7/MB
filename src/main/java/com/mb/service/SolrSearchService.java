package com.mb.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.mb.dto.DocumentoDTO;
import com.mb.dto.SearchResponseDTO;

@Service
public class SolrSearchService {

    private static final int MAX_SIZE = 50;
    private static final int SNIPPET_CHARS = 250;

    private final SolrClient solrClient;
    private final String qf;
    private final String pf;
    private final String ps;
    private final String mm;
    private final String tie;

    public SolrSearchService(SolrClient solrClient,
                             @Value("${search.qf}") String qf,
                             @Value("${search.pf}") String pf,
                             @Value("${search.ps}") String ps,
                             @Value("${search.mm}") String mm,
                             @Value("${search.tie}") String tie) {
        this.solrClient = solrClient;
        this.qf = qf;
        this.pf = pf;
        this.ps = ps;
        this.mm = mm;
        this.tie = tie;
    }

    public SearchResponseDTO search(String texto, int page, int size)
            throws SolrServerException, IOException {

        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), MAX_SIZE);
        String original = texto == null ? "" : texto.trim();
        String q = limpiarConsulta(original);

        if (q.isEmpty()) {
        	return new SearchResponseDTO(original, 0, p, s, 0, List.of());
        }

        SolrQuery query = new SolrQuery(q);
        query.set("defType", "edismax");
        query.set("qf", qf);
        query.set("pf", pf);
        query.set("ps", ps);
        query.set("mm", mm);
        query.set("tie", tie);
        query.setFields("id", "title", "text", "score");
        query.setStart((p - 1) * s);
        query.setRows(s);

        // Resaltado: Solr escapa el HTML del texto y solo deja nuestras etiquetas <mark>
        query.setHighlight(true);
        query.addHighlightField("title");
        query.addHighlightField("text");
        query.setHighlightSimplePre("<mark>");
        query.setHighlightSimplePost("</mark>");
        query.setHighlightFragsize(SNIPPET_CHARS);
        query.setHighlightSnippets(2);
        query.set("hl.encoder", "html");
        query.set("f.title.hl.fragsize", 150);
        query.set("f.title.hl.snippets", 1);

        QueryResponse rsp = solrClient.query(query);
        SolrDocumentList results = rsp.getResults();
        Map<String, Map<String, List<String>>> resaltado = rsp.getHighlighting();

        List<DocumentoDTO> docs = new ArrayList<>();
        for (SolrDocument d : results) {
            String id = primero(d.getFieldValue("id"));
            String title = primero(d.getFieldValue("title"));
            String text = primero(d.getFieldValue("text"));
            Float score = d.getFieldValue("score") instanceof Number n ? n.floatValue() : null;

            Map<String, List<String>> h = (resaltado != null) ? resaltado.get(id) : null;
            String titleHtml = fragmento(h, "title");
            if (titleHtml == null) titleHtml = HtmlUtils.htmlEscape(recortar(title, 150));
            String snippet = fragmento(h, "text");
            if (snippet == null) {
                snippet = HtmlUtils.htmlEscape(recortar(text, SNIPPET_CHARS));
            }

            docs.add(new DocumentoDTO(id, title, text, score, titleHtml, snippet));
        }

        long total = results.getNumFound(); //Aquí obtenemos de la consulta query a solr el número de documentos encontrados
        int totalPages = (int) Math.ceil((double) total / s); //Aquí calculamos las páginas necesarias (10 resultados por página)
        return new SearchResponseDTO(original, total, p, s, totalPages, docs);
    }

    //Igual que en claseConsulta: quita signos y normaliza espacios
    private static String limpiarConsulta(String texto) {
        return texto.replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
    }

    //Une los fragmentos resaltados de un campo, o devuelve null si no hay.
    private static String fragmento(Map<String, List<String>> h, String campo) {
        if (h == null) return null;
        List<String> frags = h.get(campo);
        if (frags == null || frags.isEmpty()) return null;
        return String.join(" … ", frags);
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return "";
        if (texto.length() <= max) return texto;
        int corte = texto.lastIndexOf(' ', max);
        return texto.substring(0, corte > 0 ? corte : max) + " …";
    }

    /** Los campos de Solr pueden venir como valor simple o como lista. */
    private static String primero(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Collection<?> c) {
            return c.isEmpty() ? null : String.valueOf(c.iterator().next());
        }
        return String.valueOf(valor);
    }
}
