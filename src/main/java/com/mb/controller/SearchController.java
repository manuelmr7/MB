package com.mb.controller;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mb.dto.SearchResponseDTO;
import com.mb.service.SolrSearchService;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
public class SearchController {

    private final SolrSearchService searchService;

    public SearchController(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponseDTO query(@RequestParam("q") String q,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size)
            throws SolrServerException, IOException {
        return searchService.search(q, page, size);
    }

    @ExceptionHandler({SolrServerException.class, IOException.class})
    public ResponseEntity<Map<String, String>> solrNoDisponible(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "No se pudo consultar Solr: " + e.getMessage()));
    }
}
