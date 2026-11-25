package com.mb.dto;

import java.util.List;

public record SearchResponseDTO(
        String query,
        long total,
        int page,
        int size,
        int totalPages,
        List<DocumentoDTO> docs) {
}
