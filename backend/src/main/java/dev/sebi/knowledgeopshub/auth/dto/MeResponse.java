package dev.sebi.knowledgeopshub.auth.dto;

import dev.sebi.knowledgeopshub.auth.UserRole;
import java.util.UUID;

public record MeResponse(UUID id, String email, UserRole role) {}
