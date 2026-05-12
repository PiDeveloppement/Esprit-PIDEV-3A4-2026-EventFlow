package com.example.pidev.service.sponsor;

import com.example.pidev.model.event.Event;
import com.example.pidev.service.event.EventService;
import java.util.*;
import java.util.stream.Collectors;

public class SponsorMatchingService {

    private final EventService eventService = new EventService();

    /**
     * Retourne une liste d'événements triés par pertinence pour un secteur donné.
     * @param industry le secteur d'activité du sponsor (ex: "Technologie, Marketing")
     * @return liste d'événements (les plus pertinents en premier)
     */
    public List<Event> findRelevantEvents(String industry) {
        if (industry == null || industry.isBlank()) return Collections.emptyList();

        // Découper le secteur en mots-clés (séparateurs : virgules, espaces)
        Set<String> keywords = Arrays.stream(industry.toLowerCase().split("[,\\s]+"))
                .filter(k -> k.length() > 2) // ignorer les mots trop courts
                .collect(Collectors.toSet());

        if (keywords.isEmpty()) return Collections.emptyList();

        List<Event> allEvents = eventService.getAllEvents();
        Map<Event, Integer> scores = new HashMap<>();

        for (Event event : allEvents) {
            String text = (event.getTitle() + " " + (event.getDescription() != null ? event.getDescription() : "")).toLowerCase();
            int score = 0;
            for (String kw : keywords) {
                if (text.contains(kw)) {
                    score++;
                }
            }
            if (score > 0) {
                scores.put(event, score);
            }
        }

        // Trier par score décroissant
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Event, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}