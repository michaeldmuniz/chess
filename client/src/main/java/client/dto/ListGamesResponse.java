package client.dto;

import java.util.List;
import java.util.Map;

public record ListGamesResponse(List<Map<String, Object>> games) {}
