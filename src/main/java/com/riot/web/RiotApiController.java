package com.riot.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RiotApiController {
    private final RiotApiService riotApi;
    private final GeminiAnalysisService gemini;

    public RiotApiController(RiotApiService riotApi, GeminiAnalysisService gemini) {
        this.riotApi = riotApi;
        this.gemini = gemini;
    }

    @GetMapping("/api/player")
    public Map<String, Object> player(@RequestParam(name = "riotId", defaultValue = "") String riotId) {
        return riotApi.player(riotId);
    }

    @GetMapping("/api/team")
    public Map<String, Object> team(@RequestParam(name = "matchId", defaultValue = "") String matchId,
                                    @RequestParam(name = "puuid", defaultValue = "") String puuid) {
        return riotApi.team(matchId, puuid);
    }

    @GetMapping("/api/live-game")
    public Map<String, Object> liveGame(@RequestParam(name = "puuid", defaultValue = "") String puuid,
                                        @RequestParam(name = "summonerId", defaultValue = "") String summonerId) {
        return riotApi.liveGame(puuid, summonerId);
    }

    @PostMapping("/api/ai-analysis")
    public Map<String, String> aiAnalysis(@RequestBody JsonNode request) {
        return Map.of("reply", gemini.reply(request));
    }

    @GetMapping("/api/player-profile")
    public Map<String, Object> playerProfile(@RequestParam(name = "puuid", defaultValue = "") String puuid) {
        return riotApi.playerProfile(puuid);
    }

    @GetMapping("/api/champion/{championId}")
    public Map<String, Object> champion(@PathVariable("championId") String championId) {
        return riotApi.champion(championId);
    }

    @GetMapping("/api/ranking")
    public Map<String, Object> ranking(@RequestParam(name = "tier", defaultValue = "ALL") String tier,
                                       @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return riotApi.ranking(tier, limit);
    }
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(Map.of("message", exception.getReason() == null ? status.getReasonPhrase() : exception.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception exception) {
        return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage() == null ? "서버 오류가 발생했습니다." : exception.getMessage()));
    }
}
