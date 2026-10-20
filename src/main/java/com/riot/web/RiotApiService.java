package com.riot.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RiotApiService {
    private static final String ACCOUNT_REGION = "https://asia.api.riotgames.com";
    private static final String PLATFORM_REGION = "https://kr.api.riotgames.com";
    private static final String COMMUNITY_DEFAULT = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default";
    private static final Set<String> VALID_TIERS = Set.of("CHALLENGER", "GRANDMASTER", "MASTER", "DIAMOND", "EMERALD", "PLATINUM", "GOLD", "SILVER", "BRONZE", "IRON");
    private static final Map<String, Double> TIER_SCORE = Map.ofEntries(
            Map.entry("IRON", 0d), Map.entry("BRONZE", 2d), Map.entry("SILVER", 4d),
            Map.entry("GOLD", 6d), Map.entry("PLATINUM", 8d), Map.entry("EMERALD", 10d),
            Map.entry("DIAMOND", 12d), Map.entry("MASTER", 14d),
            Map.entry("GRANDMASTER", 15.5d), Map.entry("CHALLENGER", 17d));
    private static final Map<String, Double> DIVISION_BONUS = Map.of("I", .75, "II", .5, "III", .25, "IV", 0d);

    private final ObjectMapper mapper;
    private final HttpClient client;
    private final String apiKey;
    private volatile String ddragonVersion;
    private volatile long ddragonVersionAt;
    private volatile JsonNode ddragonChampionList;
    private final Map<String, JsonNode> communityChampionCache = new ConcurrentHashMap<>();
    private final Map<String, Object> championDetailCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> profileCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> recentMatchCache = new ConcurrentHashMap<>();
    private final Map<String, Object> soloRankCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> rankingIdentityCache = new ConcurrentHashMap<>();

    public RiotApiService(ObjectMapper mapper, @Value("${riot.api-key:${RIOT_API_KEY:}}") String apiKey) {
        this.mapper = mapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public Map<String, Object> player(String riotId) {
        String[] parts = parseRiotId(riotId);
        JsonNode account = riot(ACCOUNT_REGION + "/riot/account/v1/accounts/by-riot-id/" + path(parts[0]) + "/" + path(parts[1]));
        String puuid = text(account, "puuid", "");
        JsonNode summoner = riot(PLATFORM_REGION + "/lol/summoner/v4/summoners/by-puuid/" + path(puuid));
        JsonNode league = riot(PLATFORM_REGION + "/lol/league/v4/entries/by-puuid/" + path(puuid));
        List<Map<String, Object>> ranks = rankEntries(league);
        List<Map<String, Object>> matches = recentMatches(puuid, 10);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("riotId", text(account, "gameName", "") + "#" + text(account, "tagLine", ""));
        result.put("gameName", text(account, "gameName", ""));
        result.put("tagLine", text(account, "tagLine", ""));
        result.put("puuid", puuid);
        result.put("summonerId", text(summoner, "id", ""));
        result.put("profileIconId", number(summoner, "profileIconId", 0));
        result.put("summonerLevel", number(summoner, "summonerLevel", 0));
        result.put("ranks", ranks);
        result.put("matches", matches);
        return result;
    }

    public Map<String, Object> playerProfile(String puuid) {
        String key = puuid == null ? "" : puuid.trim();
        if (key.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PUUID가 필요합니다.");
        CacheEntry cached = profileCache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.createdAt < 300_000) return cached.value;
        JsonNode account = riot(ACCOUNT_REGION + "/riot/account/v1/accounts/by-puuid/" + path(key));
        JsonNode summoner = riot(PLATFORM_REGION + "/lol/summoner/v4/summoners/by-puuid/" + path(key));
        JsonNode league = riot(PLATFORM_REGION + "/lol/league/v4/entries/by-puuid/" + path(key));
        Map<String, Object> result = new LinkedHashMap<>();
        String gameName = text(account, "gameName", "");
        String tagLine = text(account, "tagLine", "");
        result.put("riotId", gameName + "#" + tagLine);
        result.put("gameName", gameName);
        result.put("tagLine", tagLine);
        result.put("puuid", key);
        result.put("profileIconId", number(summoner, "profileIconId", 0));
        result.put("summonerLevel", number(summoner, "summonerLevel", 0));
        result.put("ranks", rankEntries(league));
        result.put("matches", recentMatches(key, 10));
        profileCache.put(key, new CacheEntry(result, System.currentTimeMillis()));
        return result;
    }

    private List<Map<String, Object>> recentMatches(String puuid, int count) {
        CacheEntry cached = recentMatchCache.get(puuid);
        if (cached != null && System.currentTimeMillis() - cached.createdAt < 120_000) {
            @SuppressWarnings("unchecked") List<Map<String, Object>> saved = (List<Map<String, Object>>) cached.value.get("matches");
            if (saved.size() >= count) return new ArrayList<>(saved.subList(0, count));
        }
        JsonNode ids = riot(ACCOUNT_REGION + "/lol/match/v5/matches/by-puuid/" + path(puuid) + "/ids?start=0&count=" + count);
        List<Map<String, Object>> result = new ArrayList<>();
        if (!ids.isArray()) return result;
        for (JsonNode id : ids) {
            try {
                JsonNode match = riot(ACCOUNT_REGION + "/lol/match/v5/matches/" + path(id.asText()));
                Map<String, Object> stats = participantStats(match, puuid);
                if (stats != null) result.add(stats);
            } catch (RuntimeException exception) {
                System.err.println("매치 조회 실패: " + exception.getMessage());
            }
        }
        recentMatchCache.put(puuid, new CacheEntry(Map.of("matches", result), System.currentTimeMillis()));
        return result;
    }

    public Map<String, Object> liveGame(String puuid, String summonerId) {
        String playerPuuid = puuid == null ? "" : puuid.trim();
        if (playerPuuid.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PUUID가 필요합니다.");

        JsonNode game;
        try {
            game = riot(PLATFORM_REGION + "/lol/spectator/v5/active-games/by-summoner/" + path(playerPuuid));
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 404) throw exception;
            System.err.println("Spectator 조회(PUUID, KR) 404: " + exception.getReason());
            return Map.of("available", false, "message", "KR 서버 Riot Spectator API가 PUUID 조회에 404를 반환했습니다. Summoner ID는 이 API 조회에 필요하지 않습니다. 게임 클라이언트 설정 > 일반 > 시스템에서 Streamer Mode의 '내 이름 숨기기'와 '내 식별 정보 숨기기'를 끄고, 실제 게임 로딩이 끝난 뒤 다시 확인해 주세요.");
        }

        JsonNode participants = game.path("participants");
        if (!participants.isArray() || participants.size() != 10) {
            return Map.of("available", false, "message", "현재 경기의 10명 참가자 정보를 완전하게 가져오지 못했습니다.");
        }

        List<Map<String, Object>> allPlayers = new ArrayList<>();
        Map<Integer, List<Map<String, Object>>> byTeam = new LinkedHashMap<>();
        int ownTeamId = 0;
        for (JsonNode participant : participants) {
            Map<String, Object> player = liveParticipant(participant);
            int teamId = (int) player.get("teamId");
            if (playerPuuid.equals(player.get("puuid"))) ownTeamId = teamId;
            allPlayers.add(player);
            byTeam.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(player);
        }
        if (ownTeamId == 0 && !byTeam.isEmpty()) ownTeamId = byTeam.keySet().iterator().next();
        List<Map<String, Object>> ownTeam = byTeam.getOrDefault(ownTeamId, List.of());
        int ownTeamKey = ownTeamId;
        int enemyTeamId = byTeam.keySet().stream().filter(teamId -> teamId != ownTeamKey).findFirst().orElse(0);
        List<Map<String, Object>> enemyTeam = byTeam.getOrDefault(enemyTeamId, List.of());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", true);
        result.put("gameId", longNumber(game, "gameId", 0));
        result.put("gameMode", text(game, "gameMode", text(game, "gameType", "League of Legends")));
        result.put("gameType", text(game, "gameType", ""));
        result.put("gameStartTime", longNumber(game, "gameStartTime", 0));
        result.put("gameLength", longNumber(game, "gameLength", 0));
        result.put("queueId", number(game, "gameQueueConfigId", 0));
        result.put("queueName", queueName(number(game, "gameQueueConfigId", 0)));
        result.put("myTeam", ownTeam);
        result.put("enemyTeam", enemyTeam);
        result.put("teamEstimate", compareTeams(ownTeam, enemyTeam));
        result.put("laneEstimates", compareLanes(ownTeam, enemyTeam));
        result.put("note", "현재 게임 참가자의 솔로랭크와 최근 최대 3경기 기록을 합산한 추정 우세도입니다. 공식 승률이나 실제 승부 확률이 아닙니다.");
        return result;
    }

    private Map<String, Object> liveParticipant(JsonNode participant) {
        String summonerId = text(participant, "summonerId", "");
        String puuid = text(participant, "puuid", "");
        JsonNode summoner = null;
        if (puuid.isBlank() && !summonerId.isBlank()) {
            try {
                summoner = riot(PLATFORM_REGION + "/lol/summoner/v4/summoners/" + path(summonerId));
                puuid = text(summoner, "puuid", "");
            } catch (RuntimeException exception) {
                System.err.println("현재 게임 참가자의 PUUID 조회 실패: " + exception.getMessage());
            }
        }

        String gameName = text(participant, "riotIdGameName", text(participant, "summonerName", "플레이어"));
        String tagLine = text(participant, "riotIdTagline", "");
        if (!puuid.isBlank()) {
            try {
                JsonNode account = riot(ACCOUNT_REGION + "/riot/account/v1/accounts/by-puuid/" + path(puuid));
                gameName = text(account, "gameName", gameName);
                tagLine = text(account, "tagLine", tagLine);
            } catch (RuntimeException exception) {
                System.err.println("현재 게임 참가자의 Riot ID 조회 실패: " + exception.getMessage());
            }
        }

        JsonNode solo = null;
        if (!puuid.isBlank()) {
            try {
                JsonNode entries = riot(PLATFORM_REGION + "/lol/league/v4/entries/by-puuid/" + path(puuid));
                for (JsonNode entry : entries) if ("RANKED_SOLO_5x5".equals(text(entry, "queueType", ""))) { solo = entry; break; }
            } catch (RuntimeException exception) {
                System.err.println("현재 게임 참가자의 랭크 조회 실패: " + exception.getMessage());
            }
        }

        List<Map<String, Object>> recent = List.of();
        if (!puuid.isBlank()) {
            try { recent = recentMatches(puuid, 3); }
            catch (RuntimeException exception) { System.err.println("최근 전적 조회 실패: " + exception.getMessage()); }
        }
        String position = text(participant, "teamPosition", "");
        if (position.isBlank()) position = primaryPosition(recent);
        int recentWins = (int) recent.stream().filter(match -> Boolean.TRUE.equals(match.get("win"))).count();
        int recentGames = recent.size();
        int seasonWins = solo == null ? 0 : number(solo, "wins", 0);
        int seasonLosses = solo == null ? 0 : number(solo, "losses", 0);
        int seasonGames = seasonWins + seasonLosses;
        double recentWinRate = recentGames > 0 ? recentWins * 100d / recentGames : 50;
        double seasonWinRate = seasonGames > 0 ? seasonWins * 100d / seasonGames : 0;
        double recentKda = recentKda(recent);
        String tier = solo == null ? "" : text(solo, "tier", "");
        String division = solo == null ? "" : text(solo, "rank", "");
        double rankScore = tier.isBlank() ? 8.5 : rankScore(tier, division);
        double kdaScore = recentGames > 0 ? Math.min(recentKda / 5d, 1) : .5;
        double powerScore = .55 * (rankScore / 17d) + .3 * (recentWinRate / 100d) + .15 * kdaScore;

        Map<String, Object> player = new LinkedHashMap<>();
        player.put("puuid", puuid);
        player.put("riotId", gameName + (tagLine.isBlank() ? "" : "#" + tagLine));
        player.put("gameName", gameName);
        player.put("teamId", number(participant, "teamId", 0));
        player.put("championId", number(participant, "championId", 0));
        player.put("championName", championName(number(participant, "championId", 0)));
        player.put("profileIconId", number(participant, "profileIconId", 0));
        player.put("rawPosition", position);
        player.put("teamPosition", positionName(position));
        player.put("rankTier", tier);
        player.put("rankDivision", division);
        player.put("rankLabel", solo == null ? "언랭크" : rankLabel(tier, division));
        player.put("leaguePoints", solo == null ? null : number(solo, "leaguePoints", 0));
        player.put("seasonWins", seasonWins);
        player.put("seasonLosses", seasonLosses);
        player.put("seasonWinRate", round1(seasonWinRate));
        player.put("recentGames", recentGames);
        player.put("recentWins", recentWins);
        player.put("recentLosses", recentGames - recentWins);
        player.put("recentWinRate", round1(recentWinRate));
        player.put("recentKda", recentKda);
        player.put("powerScore", powerScore);
        player.put("positionEstimated", text(participant, "teamPosition", "").isBlank());
        return player;
    }

    private String championName(int championId) {
        try {
            if (ddragonChampionList == null) {
                String version = ddragonVersion();
                ddragonChampionList = publicJson("https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/champion.json");
            }
            for (JsonNode champion : ddragonChampionList.path("data")) {
                if (String.valueOf(championId).equals(text(champion, "key", ""))) return text(champion, "name", "알 수 없음");
            }
        } catch (RuntimeException exception) { System.err.println("챔피언 이름 조회 실패: " + exception.getMessage()); }
        return "알 수 없음";
    }

    private String primaryPosition(List<Map<String, Object>> matches) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, Object> match : matches) {
            String position = String.valueOf(match.getOrDefault("teamPosition", ""));
            if (!position.isBlank() && !"NONE".equals(position)) counts.merge(position, 1, Integer::sum);
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
    }

    private double recentKda(List<Map<String, Object>> matches) {
        if (matches.isEmpty()) return 0;
        double kills = matches.stream().mapToDouble(m -> num(m.get("kills"))).sum();
        double deaths = matches.stream().mapToDouble(m -> num(m.get("deaths"))).sum();
        double assists = matches.stream().mapToDouble(m -> num(m.get("assists"))).sum();
        return round2((kills + assists) / Math.max(deaths, 1));
    }

    private Map<String, Object> compareTeams(List<Map<String, Object>> ownTeam, List<Map<String, Object>> enemyTeam) {
        double own = ownTeam.stream().mapToDouble(p -> num(p.get("powerScore"))).average().orElse(.5);
        double enemy = enemyTeam.stream().mapToDouble(p -> num(p.get("powerScore"))).average().orElse(.5);
        int ownRate = (int) Math.round(Math.max(20, Math.min(80, 50 + (own - enemy) * 40)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("myWinRate", ownRate); result.put("enemyWinRate", 100 - ownRate);
        result.put("myRecentWinRate", teamRecentWinRate(ownTeam)); result.put("enemyRecentWinRate", teamRecentWinRate(enemyTeam));
        result.put("myPower", round2(own)); result.put("enemyPower", round2(enemy));
        return result;
    }

    private double teamRecentWinRate(List<Map<String, Object>> team) {
        return team.stream().mapToDouble(p -> num(p.get("recentWinRate"))).average().orElse(0);
    }

    private List<Map<String, Object>> compareLanes(List<Map<String, Object>> ownTeam, List<Map<String, Object>> enemyTeam) {
        Map<String, String> positions = Map.of("TOP", "탑", "JUNGLE", "정글", "MIDDLE", "미드", "BOTTOM", "바텀", "UTILITY", "서폿", "SUPPORT", "서폿");
        List<String> order = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String position : order) {
            Map<String, Object> own = ownTeam.stream().filter(p -> position.equals(p.get("rawPosition"))).findFirst().orElse(null);
            Map<String, Object> enemy = enemyTeam.stream().filter(p -> position.equals(p.get("rawPosition"))).findFirst().orElse(null);
            if (own == null || enemy == null) continue;
            double difference = num(own.get("powerScore")) - num(enemy.get("powerScore"));
            int rate = (int) Math.round(Math.max(20, Math.min(80, 50 + difference * 40)));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("position", positions.getOrDefault(position, position));
            row.put("myPlayer", own); row.put("enemyPlayer", enemy);
            row.put("myWinRate", rate); row.put("enemyWinRate", 100 - rate);
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> participantStats(JsonNode match, String puuid) {
        JsonNode info = match.path("info");
        JsonNode participant = null;
        for (JsonNode candidate : info.path("participants")) {
            if (puuid.equals(text(candidate, "puuid", ""))) { participant = candidate; break; }
        }
        if (participant == null) return null;
        int cs = number(participant, "totalMinionsKilled", 0) + number(participant, "neutralMinionsKilled", 0);
        long duration = number(info, "gameDuration", 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchId", text(match.path("metadata"), "matchId", ""));
        result.put("gameCreation", longNumber(info, "gameCreation", 0));
        result.put("gameDuration", duration);
        result.put("gameMode", text(info, "gameMode", ""));
        result.put("queueId", number(info, "queueId", 0));
        result.put("queueName", queueName(number(info, "queueId", 0)));
        result.put("championId", number(participant, "championId", 0));
        result.put("championName", text(participant, "championName", ""));
        result.put("kills", number(participant, "kills", 0));
        result.put("deaths", number(participant, "deaths", 0));
        result.put("assists", number(participant, "assists", 0));
        result.put("win", participant.path("win").asBoolean(false));
        result.put("teamPosition", text(participant, "teamPosition", text(participant, "lane", "")));
        result.put("totalCs", cs);
        result.put("csPerMinute", duration > 0 ? round1(cs / (duration / 60d)) : 0);
        result.put("summoner1Id", number(participant, "summoner1Id", 0));
        result.put("summoner2Id", number(participant, "summoner2Id", 0));
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i <= 6; i++) items.add(number(participant, "item" + i, 0));
        result.put("items", items);
        return result;
    }

    public Map<String, Object> team(String matchId, String puuid) {
        if (matchId == null || matchId.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "matchId가 필요합니다.");
        JsonNode match = riot(ACCOUNT_REGION + "/lol/match/v5/matches/" + path(matchId.trim()));
        JsonNode info = match.path("info");
        JsonNode participants = info.path("participants");
        if (!participants.isArray() || participants.size() != 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 경기의 참가자 데이터를 충분히 가져오지 못했습니다.");
        int playerTeamId = 0;
        List<Map<String, Object>> all = new ArrayList<>();
        Map<Integer, List<Map<String, Object>>> teams = new LinkedHashMap<>();
        Map<Integer, Integer> teamKills = new HashMap<>();
        for (JsonNode p : participants) {
            int teamId = number(p, "teamId", 0);
            String id = text(p, "puuid", "");
            if (puuid != null && !puuid.isBlank() && puuid.equals(id)) playerTeamId = teamId;
            int kills = number(p, "kills", 0);
            teamKills.merge(teamId, kills, Integer::sum);
            int cs = number(p, "totalMinionsKilled", 0) + number(p, "neutralMinionsKilled", 0);
            long duration = number(info, "gameDuration", 0);
            String name = text(p, "riotIdGameName", text(p, "summonerName", "Unknown"));
            String riotId = text(p, "riotId", name + "#" + text(p, "riotIdTagline", ""));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("puuid", id); row.put("riotId", riotId.replaceAll("#$", "")); row.put("gameName", name);
            row.put("championId", number(p, "championId", 0)); row.put("championName", text(p, "championName", ""));
            row.put("teamId", teamId); row.put("teamPosition", positionName(text(p, "teamPosition", text(p, "lane", ""))));
            row.put("kills", kills); row.put("deaths", number(p, "deaths", 0)); row.put("assists", number(p, "assists", 0));
            row.put("win", p.path("win").asBoolean(false)); row.put("totalCs", cs);
            row.put("csPerMinute", duration > 0 ? round1(cs / (duration / 60d)) : 0); row.put("gameDuration", duration);
            all.add(row); teams.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(row);
        }
        if (playerTeamId == 0) {
            if (puuid == null || puuid.isBlank()) playerTeamId = number(participants.get(0), "teamId", 0);
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색한 플레이어가 선택한 경기에서 확인되지 않습니다.");
        }
        for (Map<String, Object> p : all) {
            int teamId = (int) p.get("teamId"), kills = (int) p.get("kills"), assists = (int) p.get("assists"), total = teamKills.getOrDefault(teamId, 0);
            p.put("killParticipation", total > 0 ? (int) Math.round((kills + assists) * 100d / total) : 0);
            p.put("title", playerTitle(p, total));
            Map<String, Object> rank = soloRank(String.valueOf(p.get("puuid")));
            p.put("rankLabel", rank.getOrDefault("label", "언랭크")); p.put("rankTier", rank.getOrDefault("tier", ""));
            p.put("rankDivision", rank.getOrDefault("rank", "")); p.put("rankLeaguePoints", rank.get("leaguePoints"));
            p.put("rankWins", rank.get("wins")); p.put("rankLosses", rank.get("losses")); p.put("rankScore", rank.get("score"));
        }
        List<Map<String, Object>> mine = teams.getOrDefault(playerTeamId, List.of());
        int selectedTeamId = playerTeamId;
        int enemyId = teams.keySet().stream().filter(id -> id != selectedTeamId).findFirst().orElse(0);
        List<Map<String, Object>> enemy = teams.getOrDefault(enemyId, List.of());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchId", matchId); result.put("gameCreation", longNumber(info, "gameCreation", 0));
        result.put("gameDuration", number(info, "gameDuration", 0)); result.put("queueName", queueName(number(info, "queueId", 0)));
        result.put("myTeam", mine); result.put("enemyTeam", enemy); result.put("myProfile", teamProfile(mine)); result.put("enemyProfile", teamProfile(enemy));
        result.put("winEstimate", winEstimate(mine, enemy)); result.put("myObjectives", objectives(info, playerTeamId));
        result.put("enemyObjectives", objectives(info, enemyId)); result.put("tips", teamTips(mine, enemy));
        return result;
    }

    private Map<String, Object> soloRank(String puuid) {
        if (soloRankCache.containsKey(puuid)) return (Map<String, Object>) soloRankCache.get(puuid);
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            JsonNode entries = riot(PLATFORM_REGION + "/lol/league/v4/entries/by-puuid/" + path(puuid));
            for (JsonNode entry : entries) if ("RANKED_SOLO_5x5".equals(text(entry, "queueType", ""))) {
                String tier = text(entry, "tier", ""), division = text(entry, "rank", "");
                result.put("tier", tier); result.put("rank", division); result.put("leaguePoints", number(entry, "leaguePoints", 0));
                result.put("wins", number(entry, "wins", 0)); result.put("losses", number(entry, "losses", 0));
                result.put("label", rankLabel(tier, division)); result.put("score", rankScore(tier, division)); break;
            }
        } catch (RuntimeException exception) { System.err.println("랭크 조회 실패: " + exception.getMessage()); }
        soloRankCache.put(puuid, result);
        return result;
    }

    private Map<String, Object> teamProfile(List<Map<String, Object>> team) {
        double n = Math.max(team.size(), 1), kills = team.stream().mapToDouble(p -> num(p.get("kills"))).sum();
        double cs = team.stream().mapToDouble(p -> num(p.get("csPerMinute"))).sum() / n;
        double deaths = team.stream().mapToDouble(p -> num(p.get("deaths"))).sum() / n;
        double kp = team.stream().mapToDouble(p -> kills > 0 ? (num(p.get("kills")) + num(p.get("assists"))) / kills : 0).sum() / n;
        String profile = kp >= .68 ? "교전 중심 팀" : cs >= 7.2 ? "성장 중심 팀" : deaths <= 3.2 ? "안정 운영형 팀" : "균형형 팀";
        return Map.of("avgCsPerMin", round1(cs), "avgDeaths", round1(deaths), "avgKillParticipation", (int) Math.round(kp * 100), "totalKills", (int) kills, "profile", profile);
    }

    private Map<String, Object> winEstimate(List<Map<String, Object>> mine, List<Map<String, Object>> enemy) {
        double my = averageRank(mine), foe = averageRank(enemy);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("myStrength", strength(mine)); result.put("enemyStrength", strength(enemy));
        if (Double.isNaN(my) || Double.isNaN(foe)) {
            result.put("available", false); result.put("myWinRate", 50); result.put("enemyWinRate", 50); result.put("difference", null);
            result.put("note", "두 팀의 랭크 정보를 충분히 확인하지 못해 50:50으로 표시합니다.");
        } else {
            double difference = my - foe; int rate = (int) Math.round(Math.max(20, Math.min(80, 50 + difference * 3)));
            result.put("available", true); result.put("myWinRate", rate); result.put("enemyWinRate", 100 - rate);
            result.put("difference", round2(difference));
            result.put("note", "현재 솔로랭크를 기반으로 한 프로토타입 전력 추정치입니다. 실제 승부 확률이나 공식 Riot 수치가 아닙니다.");
        }
        return result;
    }

    private Map<String, Object> strength(List<Map<String, Object>> team) {
        List<Double> scores = team.stream().map(p -> p.get("rankScore")).filter(Number.class::isInstance).map(v -> ((Number) v).doubleValue()).toList();
        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgScore", Double.isNaN(avg) ? null : round2(avg));
        result.put("rankedPlayers", scores.size()); result.put("totalPlayers", team.size());
        result.put("rankCoverage", team.isEmpty() ? 0 : (int) Math.round(scores.size() * 100d / team.size()));
        return result;
    }

    private double averageRank(List<Map<String, Object>> team) {
        return team.stream().map(p -> p.get("rankScore")).filter(Number.class::isInstance).mapToDouble(v -> ((Number) v).doubleValue()).average().orElse(Double.NaN);
    }

    private List<String> teamTips(List<Map<String, Object>> mine, List<Map<String, Object>> enemy) {
        Map<String, Object> a = teamProfile(mine), b = teamProfile(enemy);
        double ac = num(a.get("avgCsPerMin")), bc = num(b.get("avgCsPerMin")), ak = num(a.get("avgKillParticipation")), bk = num(b.get("avgKillParticipation"));
        List<String> tips = new ArrayList<>();
        tips.add(ac > bc + .6 ? "우리 팀은 평균 CS/min이 더 높아 성장 중심 성향이 보입니다." : bc > ac + .6 ? "상대 팀은 평균 CS/min이 더 높아 성장 중심 성향이 보입니다." : "양 팀의 평균 CS/min 차이가 크지 않아 성장 지표는 비슷합니다.");
        tips.add(ak > bk + 7 ? "우리 팀은 선택 경기에서 교전 참여 비율이 더 높았습니다." : bk > ak + 7 ? "상대 팀은 선택 경기에서 교전 참여 비율이 더 높았습니다." : "양 팀의 교전 참여 비율이 비슷하게 나타났습니다.");
        tips.add("아래 수치는 선택한 종료 경기의 데이터를 바탕으로 한 복기용 분석입니다.");
        return tips;
    }

    private Map<String, Object> objectives(JsonNode info, int teamId) {
        for (JsonNode team : info.path("teams")) if (number(team, "teamId", 0) == teamId) {
            JsonNode obj = team.path("objectives");
            return Map.of("baron", number(obj.path("baron"), "kills", 0), "dragon", number(obj.path("dragon"), "kills", 0), "tower", number(obj.path("tower"), "kills", 0), "inhibitor", number(obj.path("inhibitor"), "kills", 0));
        }
        return Map.of("baron", 0, "dragon", 0, "tower", 0, "inhibitor", 0);
    }

    public Map<String, Object> champion(String championId) {
        if (championId == null || !championId.matches("\\d+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 챔피언 ID가 필요합니다.");
        if (championDetailCache.containsKey(championId)) return (Map<String, Object>) championDetailCache.get(championId);
        JsonNode champion = communityChampionCache.computeIfAbsent(championId, this::communityChampion);
        if (champion == null || champion.isMissingNode() || champion.isNull()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "챔피언 " + championId + " 정보를 찾을 수 없습니다.");
        JsonNode skins = champion.path("skins"), baseSkin = null;
        for (JsonNode skin : skins) if (skin.path("isBase").asBoolean(false)) { baseSkin = skin; break; }
        if (baseSkin == null && skins.isArray() && !skins.isEmpty()) baseSkin = skins.get(0);
        JsonNode spells = champion.path("spells");
        List<String> tags = new ArrayList<>();
        for (JsonNode role : champion.path("roles")) tags.add(role.asText());
        addText(tags, champion.path("championTagInfo").path("championTagPrimary")); addText(tags, champion.path("championTagInfo").path("championTagSecondary"));
        Map<String, Object> passive = null;
        JsonNode sourcePassive = champion.path("passive");
        if (!sourcePassive.isMissingNode() && !sourcePassive.isNull()) passive = Map.of("name", text(sourcePassive, "name", "패시브"), "description", text(sourcePassive, "description", ""), "imageUrl", "");
        List<Map<String, Object>> resultSpells = new ArrayList<>();
        if (spells.isArray()) for (int i = 0; i < Math.min(4, spells.size()); i++) {
            JsonNode spell = spells.get(i); resultSpells.add(Map.of("id", text(spell, "spellKey", ""), "name", text(spell, "name", ""), "description", text(spell, "description", text(spell, "dynamicDescription", "")), "tooltip", text(spell, "dynamicDescription", ""), "imageUrl", ""));
        }
        String alias = text(champion, "alias", text(champion, "id", championId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", text(champion, "id", championId)); result.put("name", text(champion, "name", alias)); result.put("title", text(champion, "title", ""));
        result.put("blurb", text(champion, "shortBio", "")); result.put("lore", text(champion, "shortBio", "")); result.put("tags", tags);
        result.put("stats", new LinkedHashMap<>()); result.put("imageUrl", communityAsset(text(champion, "squarePortraitPath", "")));
        result.put("splashUrl", communityAsset(baseSkin == null ? "" : text(baseSkin, "splashPath", text(baseSkin, "uncenteredSplashPath", ""))));
        result.put("passive", passive); result.put("spells", resultSpells);
        try {
            String version = ddragonVersion();
            JsonNode payload = publicJson("https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/champion/" + path(alias) + ".json");
            JsonNode dd = payload.path("data").path(alias);
            if (dd.has("stats")) result.put("stats", mapper.convertValue(dd.path("stats"), Map.class));
            if (dd.has("passive")) {
                JsonNode dp = dd.path("passive"), image = dp.path("image");
                result.put("passive", Map.of("name", text(dp, "name", "패시브"), "description", text(dp, "description", ""), "imageUrl", "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/passive/" + text(image, "full", "")));
            }
            JsonNode ddSpells = dd.path("spells");
            if (ddSpells.isArray()) {
                resultSpells = new ArrayList<>();
                for (int i = 0; i < Math.min(4, ddSpells.size()); i++) {
                    JsonNode s = ddSpells.get(i), ci = s.path("image");
                    String imageUrl = ci.has("full") ? "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/spell/" + text(ci, "full", "") : "";
                    resultSpells.add(Map.of("id", text(s, "id", ""), "name", text(s, "name", ""), "description", text(s, "description", ""), "tooltip", text(s, "tooltip", ""), "imageUrl", imageUrl));
                }
                result.put("spells", resultSpells);
            }
        } catch (RuntimeException exception) { System.err.println("Data Dragon 챔피언 상세 조회 생략: " + exception.getMessage()); }
        championDetailCache.put(championId, result);
        return result;
    }

    private JsonNode communityChampion(String id) {
        for (String locale : List.of("ko_kr", "default")) {
            try { return publicJson("https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/" + locale + "/v1/champions/" + id + ".json"); }
            catch (RuntimeException ignored) { }
        }
        return null;
    }

    private String communityAsset(String assetPath) {
        String raw = assetPath == null ? "" : assetPath.trim();
        if (raw.isEmpty()) return "";
        String marker = "/lol-game-data/assets/";
        int index = raw.toLowerCase().indexOf(marker);
        if (index >= 0) return COMMUNITY_DEFAULT + "/assets/" + raw.substring(index + marker.length()).replaceFirst("^/", "").toLowerCase();
        return raw.startsWith("/") ? COMMUNITY_DEFAULT + raw : raw;
    }

    public Map<String, Object> ranking(String requestedTier, int requestedLimit) {
        String tier = requestedTier == null ? "ALL" : requestedTier.trim().toUpperCase();
        int limit = Math.min(Math.max(requestedLimit <= 0 ? 50 : requestedLimit, 1), 50);
        if (!"ALL".equals(tier) && !VALID_TIERS.contains(tier)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 랭킹 티어입니다.");
        List<Map<String, Object>> entries = new ArrayList<>(); String note;
        if ("ALL".equals(tier)) {
            for (String t : List.of("CHALLENGER", "GRANDMASTER", "MASTER")) entries.addAll(rankingEntries(t));
            Map<String, Integer> order = Map.of("CHALLENGER", 0, "GRANDMASTER", 1, "MASTER", 2);
            entries.sort(Comparator.comparingInt((Map<String, Object> e) -> order.getOrDefault(String.valueOf(e.get("tier")), 99)).thenComparing((a,b) -> Double.compare(num(b.get("leaguePoints")), num(a.get("leaguePoints")))));
            note = "KR 솔로랭크 · 챌린저·그랜드마스터·마스터 통합 상위 목록 (최대 50명)";
        } else {
            entries = rankingEntries(tier);
            entries.sort((a,b) -> Double.compare(num(b.get("leaguePoints")), num(a.get("leaguePoints"))));
            note = "CHALLENGER".equals(tier) ? "KR 솔로랭크 · 챌린저 랭킹" : "GRANDMASTER".equals(tier) ? "KR 솔로랭크 · 그랜드마스터 랭킹" : "MASTER".equals(tier) ? "KR 솔로랭크 · 마스터 랭킹" : "KR 솔로랭크 · " + tier + " I 상위 목록 (1페이지)";
        }
        List<Map<String, Object>> visible = new ArrayList<>(entries.subList(0, Math.min(limit, entries.size())));
        resolveRankingIdentities(visible);
        return Map.of("entries", visible, "note", note, "fetchedAt", System.currentTimeMillis());
    }

    private List<Map<String, Object>> rankingEntries(String tier) {
        JsonNode data;
        if (Set.of("CHALLENGER", "GRANDMASTER", "MASTER").contains(tier)) {
            String path = "CHALLENGER".equals(tier) ? "challengerleagues" : "GRANDMASTER".equals(tier) ? "grandmasterleagues" : "masterleagues";
            data = riot(PLATFORM_REGION + "/lol/league/v4/" + path + "/by-queue/RANKED_SOLO_5x5").path("entries");
        } else data = riot(PLATFORM_REGION + "/lol/league/v4/entries/RANKED_SOLO_5x5/" + tier + "/I?page=1");
        List<Map<String, Object>> result = new ArrayList<>();
        if (!data.isArray()) return result;
        for (JsonNode e : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("summonerId", text(e, "summonerId", "")); row.put("puuid", text(e, "puuid", ""));
            row.put("riotId", text(e, "riotId", "")); row.put("summonerName", text(e, "summonerName", ""));
            row.put("leaguePoints", number(e, "leaguePoints", 0)); row.put("wins", number(e, "wins", 0)); row.put("losses", number(e, "losses", 0));
            row.put("rank", text(e, "rank", "")); row.put("tier", text(e, "tier", tier)); row.put("hotStreak", e.path("hotStreak").asBoolean(false));
            row.put("veteran", e.path("veteran").asBoolean(false)); row.put("freshBlood", e.path("freshBlood").asBoolean(false)); result.add(row);
        }
        return result;
    }

    private void resolveRankingIdentities(List<Map<String, Object>> entries) {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(5, Math.max(1, entries.size())));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Map<String, Object> entry : entries) futures.add(pool.submit(() -> resolveRankingIdentity(entry)));
            for (Future<?> future : futures) future.get();
        } catch (Exception exception) { throw new RuntimeException("랭킹 플레이어 이름을 가져오지 못했습니다.", exception); }
        finally { pool.shutdown(); }
    }

    private void resolveRankingIdentity(Map<String, Object> entry) {
        String puuid = String.valueOf(entry.getOrDefault("puuid", "")), summonerId = String.valueOf(entry.getOrDefault("summonerId", ""));
        String key = !puuid.isBlank() ? puuid : !summonerId.isBlank() ? summonerId : String.valueOf(entry.getOrDefault("summonerName", ""));
        if (key.isBlank()) return;
        Map<String, Object> identity = rankingIdentityCache.get(key);
        if (identity == null) {
            try {
                String gameName = "", tagLine = "";
                if (puuid.isBlank() && !summonerId.isBlank()) {
                    JsonNode summoner = riot(PLATFORM_REGION + "/lol/summoner/v4/summoners/" + path(summonerId));
                    puuid = text(summoner, "puuid", ""); gameName = text(summoner, "name", "");
                }
                if (!puuid.isBlank()) {
                    JsonNode account = riot(ACCOUNT_REGION + "/riot/account/v1/accounts/by-puuid/" + path(puuid));
                    gameName = text(account, "gameName", gameName); tagLine = text(account, "tagLine", "");
                }
                identity = new LinkedHashMap<>();
                identity.put("puuid", puuid);
                identity.put("riotId", gameName.isBlank() ? "" : gameName + (tagLine.isBlank() ? "" : "#" + tagLine));
                identity.put("summonerName", gameName.isBlank() ? String.valueOf(entry.getOrDefault("summonerName", "")) : gameName);
                rankingIdentityCache.put(key, identity); if (!puuid.isBlank()) rankingIdentityCache.put(puuid, identity);
            } catch (RuntimeException exception) { return; }
        }
        entry.putAll(identity);
    }

    private List<Map<String, Object>> rankEntries(JsonNode entries) {
        List<Map<String, Object>> ranks = new ArrayList<>();
        if (entries.isArray()) for (JsonNode e : entries) {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("queueType", text(e, "queueType", "")); row.put("tier", text(e, "tier", ""));
            row.put("rank", text(e, "rank", "")); row.put("leaguePoints", number(e, "leaguePoints", 0)); row.put("wins", number(e, "wins", 0)); row.put("losses", number(e, "losses", 0)); ranks.add(row);
        }
        return ranks;
    }

    private JsonNode riot(String url) {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "RIOT_API_KEY 환경 변수가 설정되지 않았습니다.");
        return getJson(url, true);
    }

    private JsonNode publicJson(String url) { return getJson(url, false); }

    private JsonNode getJson(String url, boolean authenticated) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(45)).header("Accept", "application/json").GET();
            if (authenticated) builder.header("X-Riot-Token", apiKey);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = "Riot API 오류 (" + response.statusCode() + ")";
                try { message = mapper.readTree(response.body()).path("status").path("message").asText(message); } catch (Exception ignored) { }
                throw new ResponseStatusException(HttpStatusCode.valueOf(response.statusCode()), message);
            }
            return mapper.readTree(response.body());
        } catch (ResponseStatusException exception) { throw exception; }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Riot API 요청이 중단되었습니다."); }
        catch (IOException | IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Riot API 응답을 읽을 수 없습니다.", exception); }
    }

    private String ddragonVersion() {
        if (ddragonVersion != null && System.currentTimeMillis() - ddragonVersionAt < 600_000) return ddragonVersion;
        JsonNode versions = publicJson("https://ddragon.leagueoflegends.com/api/versions.json");
        if (!versions.isArray() || versions.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Data Dragon 최신 버전을 찾지 못했습니다.");
        ddragonVersion = versions.get(0).asText(); ddragonVersionAt = System.currentTimeMillis(); return ddragonVersion;
    }

    private String[] parseRiotId(String riotId) {
        String value = riotId == null ? "" : riotId.trim(); int separator = value.lastIndexOf('#');
        if (separator <= 0 || separator == value.length() - 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Riot ID를 gameName#tagLine 형식으로 입력해주세요.");
        return new String[]{value.substring(0, separator).trim(), value.substring(separator + 1).trim()};
    }

    private String queueName(int id) {
        return switch (id) { case 0 -> "커스텀"; case 420 -> "솔로랭크"; case 430 -> "일반"; case 440 -> "자유랭크"; case 450 -> "칼바람 나락"; case 490 -> "빠른 대전"; case 700 -> "격전"; case 830, 840, 850 -> "AI 상대"; case 1020 -> "단일 챔피언"; case 1400 -> "궁극기 주문서"; case 1710 -> "아레나"; default -> "게임 (" + id + ")"; };
    }

    private String positionName(String position) {
        return switch (position) { case "TOP" -> "탑"; case "JUNGLE" -> "정글"; case "MIDDLE" -> "미드"; case "BOTTOM" -> "바텀"; case "UTILITY", "SUPPORT" -> "서폿"; case "" -> "포지션 미상"; default -> position; };
    }

    private String playerTitle(Map<String, Object> p, int teamKills) {
        int kills = (int) p.get("kills"), deaths = (int) p.get("deaths"), assists = (int) p.get("assists");
        double kp = teamKills > 0 ? (kills + assists) / (double) teamKills : 0;
        double cs = num(p.get("totalCs")) / Math.max(num(p.get("gameDuration")) / 60, 1);
        if (kills >= 8) return "캐리 기여형"; if (kp >= .7 && assists >= 6) return "교전 설계자"; if (cs >= 7.5 && deaths <= 4) return "성장 집착형"; if (assists >= 10) return "팀플레이형"; if (deaths <= 2) return "안정형 플레이어"; return "균형형 플레이어";
    }

    private double rankScore(String tier, String division) {
        Double base = TIER_SCORE.get(tier.toUpperCase()); if (base == null) return 0;
        return round2(base + DIVISION_BONUS.getOrDefault(division.toUpperCase(), 0d));
    }

    private String rankLabel(String tier, String division) { return (tier + (division.isBlank() ? "" : " " + division)).trim(); }
    private static String path(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static String text(JsonNode node, String key, String fallback) { JsonNode value = node == null ? null : node.get(key); return value == null || value.isNull() ? fallback : value.asText(fallback); }
    private static int number(JsonNode node, String key, int fallback) { JsonNode value = node == null ? null : node.get(key); return value == null || !value.canConvertToInt() ? fallback : value.asInt(fallback); }
    private static long longNumber(JsonNode node, String key, long fallback) { JsonNode value = node == null ? null : node.get(key); return value == null || !value.canConvertToLong() ? fallback : value.asLong(fallback); }
    private static double num(Object value) { return value instanceof Number n ? n.doubleValue() : 0; }
    private static double round1(double value) { return Math.round(value * 10d) / 10d; }
    private static double round2(double value) { return Math.round(value * 100d) / 100d; }
    private static void addText(List<String> list, JsonNode node) { if (node != null && !node.isNull() && !node.asText().isBlank()) list.add(node.asText()); }

    private record CacheEntry(Map<String, Object> value, long createdAt) { }
}
