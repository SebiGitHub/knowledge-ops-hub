package dev.sebi.knowledgeopshub.rag;

import dev.sebi.knowledgeopshub.auth.MeController.JwtPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AskController {

  private final RagService ragService;

  public AskController(RagService ragService) {
    this.ragService = ragService;
  }

  public record AskRequest(@NotBlank String question) {}
  public record AskResponse(String answer, java.util.List<RagService.Source> sources) {}

  @PostMapping("/ask")
  public AskResponse ask(Authentication auth, @RequestBody @Valid AskRequest req) {
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    var ans = ragService.ask(p.userId(), req.question());
    return new AskResponse(ans.answer(), ans.sources());
  }
}
