package dev.sebi.knowledgeopshub.documents.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestPathRequest(
    @NotBlank String filePath,
    String title
) {}
