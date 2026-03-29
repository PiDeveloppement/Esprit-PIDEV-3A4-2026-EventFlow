package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventCategoryService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calendrier V3 - UI/UX Amélioré
 * - Légendes catégories (icon+color DB) et statuts cliquables
 * - Statut auto calculé (À venir, En cours, Terminé, Annulé)
 * - Fond coloré transparent selon statut
 * - Clic direct → vue détail
 */
public class EventCalendarController {

    @FXML private Button prevMonthBtn, nextMonthBtn, todayBtn;
    @FXML private Button addEventBtn, addNoteBtn;
    @FXML private Label monthYearLabel;
    @FXML private Button viewMoisBtn, viewSemaineBtn, viewJourBtn, viewListeBtn;
    @FXML private HBox legendeContainer, statusLegendContainer;
    @FXML private VBox calendarContainer;

    private MainController mainController;
    private EventService eventService;
    private EventCategoryService categoryService;

    private YearMonth currentMonth;
    private LocalDate currentDate;
    private Map<LocalDate, List<Event>> eventsByDate;
    private Map<Integer, EventCategory> categoriesById;
    private String currentView = "MOIS";
    private Set<EventStatus> activeStatusFilters = new HashSet<>();
    private final Map<LocalDate, List<String>> notesByDate = new HashMap<>();
    private LocalDate selectedDate;

    private enum EventStatus {
        A_VENIR("À venir", "#E3F2FD", "#1976D2"),
        EN_COURS("En cours", "#E8F5E9", "#388E3C"),
        TERMINE("Terminé", "#F3E5F5", "#7B1FA2"),
        ANNULE("Annulé", "#FFEBEE", "#D32F2F");

        final String label;
        final String bgColor;
        final String borderColor;

        EventStatus(String label, String bgColor, String borderColor) {
            this.label = label;
            this.bgColor = bgColor;
            this.borderColor = borderColor;
        }
    }

    @FXML
    public void initialize() {
        System.out.println("✅ EventCalendarController V3 - UI/UX Amélioré");

        try {
            eventService = new EventService();
            categoryService = new EventCategoryService();

            currentDate = LocalDate.now();
            currentMonth = YearMonth.from(currentDate);
            selectedDate = currentDate;
            eventsByDate = new HashMap<>();
            categoriesById = new HashMap<>();

            activeStatusFilters.add(EventStatus.A_VENIR);
            activeStatusFilters.add(EventStatus.EN_COURS);

            loadData();
            updateViewButtons();
            updateCategoryLegend();
            updateStatusLegend();
            refreshCurrentView();

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }

    private void loadData() {
        try {
            List<Event> allEvents = eventService.getAllEvents();
            eventsByDate = allEvents.stream()
                    .filter(event -> event.getStartDate() != null)
                    .collect(Collectors.groupingBy(event -> event.getStartDate().toLocalDate()));

            List<EventCategory> categories = categoryService.getAllCategories();
            for (EventCategory cat : categories) {
                categoriesById.put(cat.getId(), cat);
            }

            System.out.println("✅ Chargé: " + allEvents.size() + " événements");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement: " + e.getMessage());
        }
    }

    private EventStatus getEventStatus(Event event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = event.getStartDate();
        LocalDateTime end = event.getEndDate();

        if (start == null) {
            return EventStatus.A_VENIR;
        }
        if (end == null) {
            return now.isBefore(start) ? EventStatus.A_VENIR : EventStatus.EN_COURS;
        }
        if (now.isBefore(start)) return EventStatus.A_VENIR;
        if (now.isAfter(end)) return EventStatus.TERMINE;
        return EventStatus.EN_COURS;
    }

    private void updateCategoryLegend() {
        legendeContainer.getChildren().removeIf(node -> node instanceof HBox);

        for (EventCategory cat : categoriesById.values()) {
            HBox badge = new HBox(8);
            badge.setAlignment(Pos.CENTER);
            badge.setPadding(new Insets(6, 12, 6, 12));

            String emoji = cat.getIcon() != null ? cat.getIcon() : "📌";
            String hexColor = cat.getColor() != null ? cat.getColor() : "#64748b";

            Circle colorCircle = new Circle(6);
            try {
                colorCircle.setFill(Color.web(hexColor));
            } catch (Exception e) {
                colorCircle.setFill(Color.web("#64748b"));
            }

            Label emojiLabel = new Label(emoji);
            emojiLabel.setStyle("-fx-font-size: 13px;");

            Label nameLabel = new Label(cat.getName());
            nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");

            badge.getChildren().addAll(colorCircle, emojiLabel, nameLabel);
            badge.setStyle("-fx-background-color: #f8fafc; -fx-border-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

            legendeContainer.getChildren().add(badge);
        }
    }

    private void updateStatusLegend() {
        statusLegendContainer.getChildren().removeIf(node -> node instanceof HBox);

        for (EventStatus status : EventStatus.values()) {
            HBox badge = createStatusBadge(status);
            statusLegendContainer.getChildren().add(badge);
        }
    }

    private HBox createStatusBadge(EventStatus status) {
        HBox badge = new HBox(8);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(6, 12, 6, 12));
        badge.setCursor(javafx.scene.Cursor.HAND);

        Circle indicator = new Circle(6);
        try {
            indicator.setFill(Color.web(status.borderColor));
        } catch (Exception e) {
            indicator.setFill(Color.GRAY);
        }

        Label label = new Label(status.label);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");

        badge.getChildren().addAll(indicator, label);
        updateStatusBadgeStyle(badge, status);

        badge.setOnMouseClicked(e -> {
            if (activeStatusFilters.contains(status)) {
                activeStatusFilters.remove(status);
            } else {
                activeStatusFilters.add(status);
            }
            updateStatusBadgeStyle(badge, status);
            refreshCurrentView();
        });

        return badge;
    }

    private void updateStatusBadgeStyle(HBox badge, EventStatus status) {
        boolean isActive = activeStatusFilters.contains(status);

        if (isActive) {
            badge.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-radius: 6; -fx-border-color: %s; -fx-border-width: 2;",
                    status.bgColor, status.borderColor
            ));
        } else {
            badge.setStyle("-fx-background-color: #f1f5f9; -fx-border-radius: 6; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-opacity: 0.6;");
        }
    }

    private void updateViewButtons() {
        String activeStyle = "-fx-background-color: #1565C0; -fx-text-fill: white; -fx-padding: 8 12; -fx-font-size: 11px; -fx-border-radius: 4;";
        String inactiveStyle = "-fx-background-color: #ccc; -fx-text-fill: #666; -fx-padding: 8 12; -fx-font-size: 11px; -fx-border-radius: 4;";

        viewMoisBtn.setStyle("MOIS".equals(currentView) ? activeStyle : inactiveStyle);
        viewSemaineBtn.setStyle("SEMAINE".equals(currentView) ? activeStyle : inactiveStyle);
        viewJourBtn.setStyle("JOUR".equals(currentView) ? activeStyle : inactiveStyle);
        viewListeBtn.setStyle("LISTE".equals(currentView) ? activeStyle : inactiveStyle);
    }

    private void buildCalendar() {
        String monthYear = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        monthYearLabel.setText(monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1));

        LocalDate firstDay = currentMonth.atDay(1);
        int lengthOfMonth = currentMonth.lengthOfMonth();
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;

        // Vider complètement le container (les en-têtes sont dans le FXML)
        calendarContainer.getChildren().clear();

        HBox currentRow = null;
        int dayCounter = 0;

        for (int i = 0; i < firstDayOfWeek; i++) {
            if (currentRow == null) currentRow = createWeekRow();
            currentRow.getChildren().add(createEmptyDayCell());
            dayCounter++;
        }

        for (int day = 1; day <= lengthOfMonth; day++) {
            if (currentRow == null) currentRow = createWeekRow();
            LocalDate cellDate = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), day);
            currentRow.getChildren().add(createDayCell(cellDate));
            dayCounter++;

            if (dayCounter % 7 == 0) {
                calendarContainer.getChildren().add(currentRow);
                currentRow = null;
            }
        }

        if (currentRow != null) {
            while (currentRow.getChildren().size() < 7) {
                currentRow.getChildren().add(createEmptyDayCell());
            }
            calendarContainer.getChildren().add(currentRow);
        }
    }

    private HBox createWeekRow() {
        HBox row = new HBox(0);
        row.setMinHeight(120);
        row.setPrefHeight(120);
        return row;
    }

    private VBox createEmptyDayCell() {
        VBox cell = new VBox();
        cell.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 1 1 0; -fx-background-color: #fafafa;");
        HBox.setHgrow(cell, Priority.ALWAYS);
        cell.setMinWidth(140);
        cell.setPrefHeight(120);
        return cell;
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(4);
        cell.setPadding(new Insets(8));
        boolean isSelected = date.equals(selectedDate);
        cell.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 1 1 0; -fx-background-color: "
                + (isSelected ? "#eef6ff" : "white") + ";");
        cell.setMinWidth(140);
        cell.setPrefHeight(120);
        HBox.setHgrow(cell, Priority.ALWAYS);

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #1565C0; -fx-padding: 4 8; -fx-border-radius: 4; -fx-background-radius: 4;");
        }

        cell.getChildren().add(dayLabel);

        List<Event> dayEvents = eventsByDate.getOrDefault(date, new ArrayList<>())
                .stream()
                .filter(event -> activeStatusFilters.contains(getEventStatus(event)))
                .sorted(Comparator.comparing(Event::getStartDate))
                .collect(Collectors.toList());

        int displayedCount = 0;
        for (Event event : dayEvents) {
            if (displayedCount >= 3) break;
            cell.getChildren().add(createEventChip(event));
            displayedCount++;
        }

        if (dayEvents.size() > 3) {
            Label moreLabel = new Label("+" + (dayEvents.size() - 3) + " plus");
            moreLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 2 0 0 0;");
            cell.getChildren().add(moreLabel);
        }

        List<String> notes = notesByDate.getOrDefault(date, Collections.emptyList());
        if (!notes.isEmpty()) {
            String noteText = notes.get(0);
            if (noteText.length() > 18) {
                noteText = noteText.substring(0, 18) + "...";
            }
            Label noteLabel = new Label("📝 " + noteText + (notes.size() > 1 ? " (+" + (notes.size() - 1) + ")" : ""));
            noteLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #0f766e; -fx-background-color: #ecfeff; -fx-padding: 2 4; -fx-background-radius: 4;");
            cell.getChildren().add(noteLabel);
        }

        cell.setOnMouseClicked(event -> {
            selectedDate = date;
            if (event.getClickCount() == 2) {
                handleAddNote();
            } else {
                refreshCurrentView();
            }
        });

        return cell;
    }

    private HBox createEventChip(Event event) {
        HBox chip = new HBox(4);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(3, 6, 3, 6));
        chip.setCursor(javafx.scene.Cursor.HAND);

        EventStatus status = getEventStatus(event);
        chip.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-radius: 4; -fx-border-color: %s; -fx-border-width: 1;",
                status.bgColor, status.borderColor
        ));

        EventCategory cat = categoriesById.get(event.getCategoryId());
        String emoji = cat != null && cat.getIcon() != null ? cat.getIcon() : "📌";
        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 11px;");

        String timeStr = event.getStartDate() != null
                ? event.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        String titleStr = event.getTitle();
        if (titleStr.length() > 15) {
            titleStr = titleStr.substring(0, 15) + "...";
        }

        Label textLabel = new Label(timeStr + " " + titleStr);
        textLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #1e293b; -fx-font-weight: 500;");
        textLabel.setMaxWidth(Double.MAX_VALUE);
        textLabel.setWrapText(false);
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        chip.getChildren().addAll(iconLabel, textLabel);

        chip.setOnMouseClicked(e -> {
            e.consume();
            if (mainController != null) {
                mainController.showEventView(event);
            }
        });

        return chip;
    }

    private void buildWeekView() {
        calendarContainer.getChildren().clear();

        LocalDate weekStart = getWeekStart(currentDate != null ? currentDate : LocalDate.now());
        monthYearLabel.setText("Semaine du " + weekStart.format(DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH)));

        Label titleLabel = new Label("📆 Semaine du " + weekStart.format(DateTimeFormatter.ofPattern("dd MMM")) +
                " au " + weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15; -fx-text-fill: #1e293b;");
        calendarContainer.getChildren().add(titleLabel);

        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = weekStart.plusDays(i);

            List<Event> dayEvents = eventsByDate.getOrDefault(dayDate, new ArrayList<>())
                    .stream()
                    .filter(event -> activeStatusFilters.contains(getEventStatus(event)))
                    .collect(Collectors.toList());

            if (!dayEvents.isEmpty()) {
                VBox dayBox = createWeekDayBox(dayDate, dayEvents);
                calendarContainer.getChildren().add(dayBox);
            }
        }
    }

    private VBox createWeekDayBox(LocalDate date, List<Event> events) {
        VBox dayBox = new VBox(10);
        dayBox.setPadding(new Insets(15));
        dayBox.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-margin: 5;");

        String dayName = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH));
        String dayNum = date.format(DateTimeFormatter.ofPattern("dd"));

        Label dayLabel = new Label(dayName.substring(0, 1).toUpperCase() + dayName.substring(1) + " " + dayNum);
        dayLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
        dayBox.getChildren().add(dayLabel);

        for (Event event : events) {
            HBox eventBox = createWeekEventBox(event);
            dayBox.getChildren().add(eventBox);
        }

        return dayBox;
    }

    private HBox createWeekEventBox(Event event) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));
        box.setCursor(javafx.scene.Cursor.HAND);

        EventStatus status = getEventStatus(event);
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-radius: 6; -fx-border-color: %s; -fx-border-width: 1;",
                status.bgColor, status.borderColor
        ));

        EventCategory cat = categoriesById.get(event.getCategoryId());
        String emoji = cat != null && cat.getIcon() != null ? cat.getIcon() : "📌";
        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 14px;");

        VBox textBox = new VBox(3);
        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String startStr = event.getStartDate() != null
                ? event.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        String endStr = event.getEndDate() != null
                ? event.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        String timeStr = startStr + " - " + endStr;
        Label timeLabel = new Label("🕐 " + timeStr);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        textBox.getChildren().addAll(titleLabel, timeLabel);
        box.getChildren().addAll(iconLabel, textBox);

        box.setOnMouseClicked(e -> {
            if (mainController != null) {
                mainController.showEventView(event);
            }
        });

        return box;
    }

    private void buildDayView() {
        calendarContainer.getChildren().clear();

        LocalDate today = currentDate != null ? currentDate : LocalDate.now();
        monthYearLabel.setText(today.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH)));
        Label titleLabel = new Label("📄 " + today.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH)));
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-text-fill: #1e293b;");
        calendarContainer.getChildren().add(titleLabel);

        List<Event> dayEvents = eventsByDate.getOrDefault(today, new ArrayList<>())
                .stream()
                .filter(event -> activeStatusFilters.contains(getEventStatus(event)))
                .sorted(Comparator.comparing(Event::getStartDate))
                .collect(Collectors.toList());

        if (dayEvents.isEmpty()) {
            Label noEvents = new Label("Aucun événement aujourd'hui");
            noEvents.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-style: italic; -fx-padding: 20;");
            calendarContainer.getChildren().add(noEvents);
        } else {
            for (Event event : dayEvents) {
                VBox eventBox = createDayEventBox(event);
                calendarContainer.getChildren().add(eventBox);
            }
        }

        List<String> notes = notesByDate.getOrDefault(today, Collections.emptyList());
        if (!notes.isEmpty()) {
            Label notesTitle = new Label("📝 Notes");
            notesTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15 0 5 0; -fx-text-fill: #0f172a;");
            calendarContainer.getChildren().add(notesTitle);
            for (String note : notes) {
                Label noteLabel = new Label("• " + note);
                noteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-padding: 3 0 3 8;");
                noteLabel.setWrapText(true);
                calendarContainer.getChildren().add(noteLabel);
            }
        }
    }

    private VBox createDayEventBox(Event event) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-margin: 10 0;");
        box.setCursor(javafx.scene.Cursor.HAND);

        EventStatus status = getEventStatus(event);
        EventCategory cat = categoriesById.get(event.getCategoryId());
        String emoji = cat != null && cat.getIcon() != null ? cat.getIcon() : "📌";

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 20px;");

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label statusBadge = new Label(status.label);
        statusBadge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 4 12; -fx-border-radius: 12; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;",
                status.bgColor, status.borderColor
        ));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(emojiLabel, titleLabel, spacer, statusBadge);
        box.getChildren().add(headerBox);

        String startStr = event.getStartDate() != null
                ? event.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        String endStr = event.getEndDate() != null
                ? event.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        String timeStr = startStr + " - " + endStr;
        Label timeLabel = new Label("🕐 " + timeStr);
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        box.getChildren().add(timeLabel);

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            Label locationLabel = new Label("📍 " + event.getLocation());
            locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            box.getChildren().add(locationLabel);
        }

        box.setOnMouseClicked(e -> {
            if (mainController != null) {
                mainController.showEventView(event);
            }
        });

        return box;
    }

    private void buildListView() {
        calendarContainer.getChildren().clear();
        String monthYear = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        monthYearLabel.setText(monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1));

        Label titleLabel = new Label("📋 Tous les événements - " + monthYearLabel.getText());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-text-fill: #1e293b;");
        calendarContainer.getChildren().add(titleLabel);

        List<Event> allMonthEvents = eventsByDate.entrySet().stream()
                .filter(entry -> entry.getKey().getYear() == currentMonth.getYear() &&
                        entry.getKey().getMonth() == currentMonth.getMonth())
                .flatMap(entry -> entry.getValue().stream())
                .filter(event -> activeStatusFilters.contains(getEventStatus(event)))
                .sorted(Comparator.comparing(Event::getStartDate))
                .collect(Collectors.toList());

        if (allMonthEvents.isEmpty()) {
            Label noEvents = new Label("Aucun événement ce mois");
            noEvents.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-style: italic; -fx-padding: 20;");
            calendarContainer.getChildren().add(noEvents);
        } else {
            LocalDate lastDate = null;
            for (Event event : allMonthEvents) {
                LocalDate eventDate = event.getStartDate().toLocalDate();
                if (!eventDate.equals(lastDate)) {
                    Label dateHeader = new Label(eventDate.format(DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH)));
                    dateHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 0 5 0; -fx-text-fill: #1565C0;");
                    calendarContainer.getChildren().add(dateHeader);
                    lastDate = eventDate;
                }

                HBox eventBox = createListEventBox(event);
                calendarContainer.getChildren().add(eventBox);
            }
        }
    }

    private HBox createListEventBox(Event event) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.setCursor(javafx.scene.Cursor.HAND);

        EventStatus status = getEventStatus(event);
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-radius: 6; -fx-border-color: %s; -fx-border-width: 1; -fx-margin: 5 0;",
                status.bgColor, status.borderColor
        ));

        EventCategory cat = categoriesById.get(event.getCategoryId());
        String emoji = cat != null && cat.getIcon() != null ? cat.getIcon() : "📌";
        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 16px;");

        VBox textBox = new VBox(3);
        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String timeStr = event.getStartDate() != null
                ? event.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        Label timeLabel = new Label("🕐 " + timeStr);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        textBox.getChildren().addAll(titleLabel, timeLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label statusLabel = new Label(status.label);
        statusLabel.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 4 10; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 9px; -fx-font-weight: bold;",
                "white", status.borderColor
        ));

        box.getChildren().addAll(iconLabel, textBox, statusLabel);

        box.setOnMouseClicked(e -> {
            if (mainController != null) {
                mainController.showEventView(event);
            }
        });

        return box;
    }

    @FXML private void handlePrevMonth() {
        if ("SEMAINE".equals(currentView)) {
            currentDate = (currentDate != null ? currentDate : LocalDate.now()).minusWeeks(1);
            currentMonth = YearMonth.from(currentDate);
        } else if ("JOUR".equals(currentView)) {
            currentDate = (currentDate != null ? currentDate : LocalDate.now()).minusDays(1);
            currentMonth = YearMonth.from(currentDate);
        } else {
            currentMonth = currentMonth.minusMonths(1);
            currentDate = currentMonth.atDay(1);
        }
        refreshCurrentView();
    }

    @FXML private void handleNextMonth() {
        if ("SEMAINE".equals(currentView)) {
            currentDate = (currentDate != null ? currentDate : LocalDate.now()).plusWeeks(1);
            currentMonth = YearMonth.from(currentDate);
        } else if ("JOUR".equals(currentView)) {
            currentDate = (currentDate != null ? currentDate : LocalDate.now()).plusDays(1);
            currentMonth = YearMonth.from(currentDate);
        } else {
            currentMonth = currentMonth.plusMonths(1);
            currentDate = currentMonth.atDay(1);
        }
        refreshCurrentView();
    }

    @FXML private void handleToday() {
        currentDate = LocalDate.now();
        selectedDate = currentDate;
        currentMonth = YearMonth.from(currentDate);
        refreshCurrentView();
    }

    @FXML private void handleViewMois() {
        currentView = "MOIS";
        updateViewButtons();
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        if (selectedDate == null) {
            selectedDate = currentDate;
        }
        currentMonth = YearMonth.from(currentDate);
        refreshCurrentView();
    }

    @FXML private void handleViewSemaine() {
        currentView = "SEMAINE";
        updateViewButtons();
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        if (selectedDate == null) {
            selectedDate = currentDate;
        }
        refreshCurrentView();
    }

    @FXML private void handleViewJour() {
        currentView = "JOUR";
        updateViewButtons();
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        if (selectedDate == null) {
            selectedDate = currentDate;
        }
        refreshCurrentView();
    }

    @FXML private void handleViewListe() {
        currentView = "LISTE";
        updateViewButtons();
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        if (selectedDate == null) {
            selectedDate = currentDate;
        }
        currentMonth = YearMonth.from(currentDate);
        refreshCurrentView();
    }

    @FXML
    private void handleAddEvent() {
        if (mainController != null) {
            mainController.showEventForm(null);
            return;
        }
        new Alert(Alert.AlertType.WARNING, "Navigation indisponible pour ajouter un événement.", ButtonType.OK).showAndWait();
    }

    @FXML
    private void handleAddNote() {
        LocalDate targetDate = selectedDate != null ? selectedDate : (currentDate != null ? currentDate : LocalDate.now());
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ajouter une note");
        dialog.setHeaderText("Note pour le " + targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dialog.setContentText("Votre note:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String note = result.get().trim();
            if (!note.isEmpty()) {
                notesByDate.computeIfAbsent(targetDate, k -> new ArrayList<>()).add(note);
                refreshCurrentView();
            }
        }
    }

    private void refreshCurrentView() {
        loadData();
        if ("SEMAINE".equals(currentView)) {
            buildWeekView();
        } else if ("JOUR".equals(currentView)) {
            buildDayView();
        } else if ("LISTE".equals(currentView)) {
            buildListView();
        } else {
            buildCalendar();
        }
    }

    private LocalDate getWeekStart(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        return date.minusDays(dayOfWeek);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
