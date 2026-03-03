package com.example.pidev.service.chart;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class QuickChartService {

    // Pas de width/height dans l'URL de base — passés dynamiquement
    private static final String BASE_URL =
            "https://quickchart.io/chart?devicePixelRatio=1&format=png&backgroundColor=white";

    private static final String[] BORDER_COLORS = {
            "#2563eb", "#f97316", "#10b981", "#7c3aed", "#ef4444", "#14b8a6", "#ec4899"
    };
    private static final String[] FILL_COLORS = {
            "rgba(37,99,235,0.82)", "rgba(249,115,22,0.82)", "rgba(16,185,129,0.82)",
            "rgba(124,58,237,0.82)", "rgba(239,68,68,0.82)", "rgba(20,184,166,0.82)",
            "rgba(236,72,153,0.82)"
    };
    private static final String[] PIE_COLORS = {
            "#7c3aed","#10b981","#3b82f6","#f59e0b","#ef4444","#14b8a6","#ec4899","#6366f1"
    };

    // ── URL builders ──────────────────────────────────────────────────────────

    /** URL avec dimensions dynamiques = taille réelle du StackPane JavaFX */
    public static String getChartUrl(JsonObject config, int width, int height) {
        String encoded = URLEncoder.encode(config.toString(), StandardCharsets.UTF_8);
        return BASE_URL
                + "&width="  + Math.max(100, width)
                + "&height=" + Math.max(100, height)
                + "&c=" + encoded;
    }

    /** Surcharge de compatibilité */
    public static String getChartUrl(JsonObject config) {
        return getChartUrl(config, 600, 340);
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    public static JsonObject createMultiBarChart(String title, String[] labels,
                                                 String[] seriesNames, double[][] seriesData) {
        JsonObject config = new JsonObject();
        config.addProperty("type", "bar");

        JsonObject data = new JsonObject();
        data.add("labels", toStringArray(labels));
        JsonArray datasets = new JsonArray();
        for (int i = 0; i < seriesNames.length; i++) {
            JsonObject ds = new JsonObject();
            ds.addProperty("label",              seriesNames[i]);
            ds.add("data",                       toDoubleArray(seriesData[i]));
            ds.addProperty("backgroundColor",    FILL_COLORS[i % FILL_COLORS.length]);
            ds.addProperty("borderColor",        BORDER_COLORS[i % BORDER_COLORS.length]);
            ds.addProperty("borderWidth",        2);
            ds.addProperty("borderRadius",       6);
            ds.addProperty("borderSkipped",      false);
            ds.addProperty("maxBarThickness",    50);
            ds.addProperty("categoryPercentage", 0.72);
            ds.addProperty("barPercentage",      0.84);
            datasets.add(ds);
        }
        data.add("datasets", datasets);
        config.add("data", data);

        JsonObject options = baseOptions();
        options.add("plugins", plugins(title));
        options.add("scales",  barScales());
        config.add("options", options);
        return config;
    }

    public static JsonObject createLineChart(String title, String[] labels,
                                             String[] seriesNames, double[][] seriesData) {
        JsonObject config = new JsonObject();
        config.addProperty("type", "line");

        JsonObject data = new JsonObject();
        data.add("labels", toStringArray(labels));
        JsonArray datasets = new JsonArray();
        for (int i = 0; i < seriesNames.length; i++) {
            JsonObject ds   = new JsonObject();
            String     name = seriesNames[i];
            ds.addProperty("label",                name);
            ds.add("data",                         toLineDataArray(name, seriesData[i]));
            ds.addProperty("borderColor",          BORDER_COLORS[i % BORDER_COLORS.length]);
            ds.addProperty("backgroundColor",      BORDER_COLORS[i % BORDER_COLORS.length]);
            ds.addProperty("fill",                 false);
            ds.addProperty("tension",              0.34);
            ds.addProperty("borderWidth",          3);
            ds.addProperty("pointRadius",          5);
            ds.addProperty("pointHoverRadius",     7);
            ds.addProperty("pointBackgroundColor", BORDER_COLORS[i % BORDER_COLORS.length]);
            ds.addProperty("pointBorderColor",     "#ffffff");
            ds.addProperty("pointBorderWidth",     2);
            ds.addProperty("spanGaps",             true);
            if (name != null && name.toLowerCase().contains("projection")) {
                JsonArray dash = new JsonArray(); dash.add(8); dash.add(6);
                ds.add("borderDash", dash);
            }
            datasets.add(ds);
        }
        data.add("datasets", datasets);
        config.add("data", data);

        JsonObject options = baseOptions();
        options.add("plugins", plugins(title));
        options.add("scales",  lineScales());
        config.add("options", options);
        return config;
    }

    public static JsonObject createDoughnutChart(String title, String[] labels, double[] values) {
        JsonObject config = new JsonObject();
        config.addProperty("type", "doughnut");
        config.add("data", pieData(labels, values));
        JsonObject options = baseOptions();
        options.add("plugins", plugins(title));
        options.addProperty("cutout", "58%");
        config.add("options", options);
        return config;
    }

    public static JsonObject createPieChart(String title, String[] labels, double[] values) {
        JsonObject config = new JsonObject();
        config.addProperty("type", "pie");
        config.add("data", pieData(labels, values));
        JsonObject options = baseOptions();
        options.add("plugins", plugins(title));
        config.add("options", options);
        return config;
    }

    // ── Builders internes ─────────────────────────────────────────────────────

    private static JsonObject pieData(String[] labels, double[] values) {
        JsonObject d = new JsonObject();
        d.add("labels", toStringArray(labels));
        JsonArray  ds  = new JsonArray();
        JsonObject dso = new JsonObject();
        dso.add("data",            toDoubleArray(values));
        dso.add("backgroundColor", toColorArray(PIE_COLORS));
        dso.addProperty("borderColor", "#ffffff");
        dso.addProperty("borderWidth",  3);
        dso.addProperty("hoverOffset", 10);
        ds.add(dso);
        d.add("datasets", ds);
        return d;
    }

    private static JsonObject baseOptions() {
        JsonObject o = new JsonObject();
        o.addProperty("responsive",          false); // ← taille fixée par l'URL
        o.addProperty("maintainAspectRatio", false);
        o.addProperty("animation",           false);
        JsonObject layout = new JsonObject();
        JsonObject pad    = new JsonObject();
        pad.addProperty("top", 10); pad.addProperty("right", 14);
        pad.addProperty("bottom", 10); pad.addProperty("left", 14);
        layout.add("padding", pad);
        o.add("layout", layout);
        return o;
    }

    private static JsonObject plugins(String titleText) {
        JsonObject p = new JsonObject();

        JsonObject title = new JsonObject();
        title.addProperty("display", titleText != null && !titleText.isEmpty());
        title.addProperty("text",    titleText == null ? "" : titleText);
        title.addProperty("color",   "#0f172a");
        title.addProperty("align",   "start");
        title.addProperty("padding", 8);
        title.add("font", font(15, true));
        p.add("title", title);

        JsonObject legend = new JsonObject();
        legend.addProperty("display",  true);
        legend.addProperty("position", "top");
        JsonObject ll = new JsonObject();
        ll.addProperty("color",         "#1e293b");
        ll.addProperty("usePointStyle", true);
        ll.addProperty("boxWidth",      13);
        ll.addProperty("padding",       14);
        ll.add("font", font(13, true));
        legend.add("labels", ll);
        p.add("legend", legend);

        JsonObject tt = new JsonObject();
        tt.addProperty("backgroundColor", "rgba(15,23,42,0.96)");
        tt.addProperty("titleColor",      "#ffffff");
        tt.addProperty("bodyColor",       "#e2e8f0");
        tt.addProperty("padding",         12);
        tt.addProperty("cornerRadius",    10);
        tt.addProperty("displayColors",   true);
        tt.add("titleFont", font(13, true));
        tt.add("bodyFont",  font(12, false));
        p.add("tooltip", tt);

        return p;
    }

    private static JsonObject barScales() {
        JsonObject s = new JsonObject();

        JsonObject x = new JsonObject();
        JsonObject xg = new JsonObject(); xg.addProperty("display", false); x.add("grid", xg);
        JsonObject xt = new JsonObject();
        xt.addProperty("color", "#1e293b"); xt.addProperty("maxRotation", 15);
        xt.addProperty("minRotation", 0);   xt.addProperty("autoSkip", false);
        xt.add("font", font(12, true)); x.add("ticks", xt);

        JsonObject y = new JsonObject(); y.addProperty("beginAtZero", true);
        JsonObject yg = new JsonObject();
        yg.addProperty("color", "#e2e8f0"); yg.addProperty("drawBorder", false); yg.addProperty("lineWidth", 1.2);
        y.add("grid", yg);
        JsonObject yt = new JsonObject();
        yt.addProperty("color", "#1e293b"); yt.addProperty("padding", 6);
        yt.add("font", font(12, true)); y.add("ticks", yt);

        s.add("x", x); s.add("y", y);
        return s;
    }

    private static JsonObject lineScales() {
        JsonObject s = new JsonObject();

        JsonObject x = new JsonObject();
        JsonObject xg = new JsonObject();
        xg.addProperty("color", "#f1f5f9"); xg.addProperty("drawBorder", false); xg.addProperty("lineWidth", 1.1);
        x.add("grid", xg);
        JsonObject xt = new JsonObject();
        xt.addProperty("color", "#1e293b"); xt.addProperty("autoSkip", true);
        xt.addProperty("maxTicksLimit", 8); xt.addProperty("maxRotation", 15); xt.addProperty("minRotation", 0);
        xt.add("font", font(12, true)); x.add("ticks", xt);

        JsonObject y = new JsonObject();
        JsonObject yg = new JsonObject();
        yg.addProperty("color", "#e2e8f0"); yg.addProperty("drawBorder", false); yg.addProperty("lineWidth", 1.2);
        y.add("grid", yg);
        JsonObject yt = new JsonObject();
        yt.addProperty("color", "#1e293b"); yt.addProperty("padding", 6);
        yt.add("font", font(12, true)); y.add("ticks", yt);

        s.add("x", x); s.add("y", y);
        return s;
    }

    private static JsonObject font(int size, boolean bold) {
        JsonObject f = new JsonObject();
        f.addProperty("size",   size);
        f.addProperty("family", "Arial");
        if (bold) f.addProperty("weight", "bold");
        return f;
    }

    private static JsonArray toStringArray(String[] v) {
        JsonArray a = new JsonArray();
        if (v == null) return a;
        for (String s : v) a.add(s == null ? "" : s);
        return a;
    }

    private static JsonArray toDoubleArray(double[] v) {
        JsonArray a = new JsonArray();
        if (v == null) return a;
        for (double d : v) a.add(d);
        return a;
    }

    private static JsonArray toColorArray(String[] v) {
        JsonArray a = new JsonArray();
        for (String s : v) a.add(s);
        return a;
    }

    private static JsonArray toLineDataArray(String seriesName, double[] values) {
        JsonArray arr = new JsonArray();
        if (values == null) return arr;

        boolean isProjection = seriesName != null && seriesName.toLowerCase().contains("projection");
        boolean isHistorical = seriesName != null && seriesName.toLowerCase().contains("historique");

        int firstNZ = -1, lastNZ = -1;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i]) > 0.000001) {
                if (firstNZ == -1) firstNZ = i;
                lastNZ = i;
            }
        }

        for (int i = 0; i < values.length; i++) {
            boolean makeNull = (isProjection && firstNZ != -1 && i < firstNZ && Math.abs(values[i]) < 0.000001)
                    || (isHistorical && lastNZ != -1 && i > lastNZ && Math.abs(values[i]) < 0.000001);
            if (makeNull) {
                arr.add(JsonNull.INSTANCE);
            } else {
                arr.add(values[i]);
            }
        }
        return arr;
    }
}