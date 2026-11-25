package com.mb.dto;

/**
 * Un resultado de búsqueda.
 *
 * title / text            -> valores originales (texto plano)
 * titleHtml / snippetHtml -> versión ya escapada para HTML, con los términos
 *                            de la consulta marcados con la etiqueta mark
 */
public record DocumentoDTO(
        String id,
        String title,
        String text,
        Float score,
        String titleHtml,
        String snippetHtml) {
}
