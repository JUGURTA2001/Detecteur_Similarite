package ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import algo.Level;
import algo.SimilarityAlgorithm;
import io.FileLoader;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.SourceFile;
import service.CodeStats;
import util.Constants;

public class MainView {

    private static final double HIGH = 70.0;
    private static final double MEDIUM = 40.0;

    @FXML private MenuBar menuBar;
    @FXML private ListView<SourceFile> referenceList;
    @FXML private ListView<SourceFile> candidatesList;
    @FXML private TextArea previewArea;
    @FXML private Label previewTitle;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> algoCombo;
    @FXML private Button compareButton;

    // tableau de bord
    @FXML private TabPane tabPane;
    @FXML private Tab previewTab;
    @FXML private Label kpiCount;
    @FXML private Label kpiMax;
    @FXML private Label kpiAvg;
    @FXML private Label kpiMin;
    @FXML private Label kpiSkipped;

    @FXML private TableView<Row> resultsTable;
    @FXML private TableColumn<Row, Number> colRank;
    @FXML private TableColumn<Row, String> colFile;
    @FXML private TableColumn<Row, String> colLang;
    @FXML private TableColumn<Row, Number> colTokens;
    @FXML private TableColumn<Row, Number> colKeywords;
    @FXML private TableColumn<Row, Number> colVars;
    @FXML private TableColumn<Row, Number> colScore;
    @FXML private TableColumn<Row, String> colLevel;

    @FXML private BarChart<String, Number> scoreChart;
    @FXML private PieChart compositionPie;
    @FXML private BarChart<String, Number> compareChart;
    @FXML private Label selectionLabel;
    @FXML private TableView<KeywordRow> keywordTable;
    @FXML private TableColumn<KeywordRow, String> kwWord;
    @FXML private TableColumn<KeywordRow, Number> kwRef;
    @FXML private TableColumn<KeywordRow, Number> kwCand;

    // ---------- Lignes des tableaux ----------

    private static final class Row {
        int rank;
        final SourceFile file;
        final double score;
        final CodeStats stats;

        Row(SourceFile file, double score, CodeStats stats) {
            this.file = file;
            this.score = score;
            this.stats = stats;
        }
    }

    private static final class KeywordRow {
        final String word;
        final int ref;
        final int cand;

        KeywordRow(String word, int ref, int cand) {
            this.word = word;
            this.ref = ref;
            this.cand = cand;
        }
    }

    // ---------- Initialisation ----------

    @FXML
    private void initialize() {
        algoCombo.getItems().addAll(
            "Niveau 1 - Comparaison textuelle",
            "Niveau 2 - Avec transformations",
            "Niveau 3 - Langages différents");
        algoCombo.getSelectionModel().selectFirst();

        referenceList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                candidatesList.getSelectionModel().clearSelection();
                showPreview(sel);
            }
        });
        candidatesList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                referenceList.getSelectionModel().clearSelection();
                showPreview(sel);
            }
        });

        scoreChart.setAnimated(false);
        compareChart.setAnimated(false);
        compositionPie.setAnimated(false);

        setupResultsTable();
        setupKeywordTable();
        updateState();
    }

    private void setupResultsTable() {
        colRank.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().rank));
        colFile.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().file.getName()));
        colLang.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().file.getLanguage())));
        colTokens.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().stats.tokens));
        colKeywords.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().stats.keywords));
        colVars.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().stats.variables));
        colScore.setCellValueFactory(c -> new ReadOnlyDoubleWrapper(c.getValue().score));
        colLevel.setCellValueFactory(c -> new ReadOnlyStringWrapper(levelName(c.getValue().score)));

        // score affiché sous forme de barre colorée
        colScore.setCellFactory(col -> new TableCell<Row, Number>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label text = new Label();
            private final HBox box = new HBox(8, bar, text);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                bar.setPrefWidth(110);
            }

            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                double s = value.doubleValue();
                bar.setProgress(s / 100.0);
                bar.setStyle("-fx-accent: " + color(s) + ";");
                text.setText(String.format("%.2f %%", s));
                setGraphic(box);
            }
        });

        resultsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> showDetails(sel));
    }

    private void setupKeywordTable() {
        kwWord.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().word));
        kwRef.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().ref));
        kwCand.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().cand));
    }

    // ---------- Menu Fichier ----------

    @FXML
    private void onChooseReference() {
        FileChooser fc = newFileChooser("Choisir le fichier de référence");
        File f = fc.showOpenDialog(owner());
        if (f == null) return;
        try {
            SourceFile sf = FileLoader.load(f.toPath());
            referenceList.getItems().setAll(sf);
            referenceList.getSelectionModel().select(sf);
            setStatus("Référence : " + sf.getName());
            clearResults();
        } catch (IOException e) {
            showError("Lecture impossible : " + e.getMessage());
        }
        updateState();
    }

    @FXML
    private void onAddCandidates() {
        FileChooser fc = newFileChooser("Ajouter des fichiers à comparer");
        List<File> files = fc.showOpenMultipleDialog(owner());
        if (files == null) return;
        int added = 0;
        for (File f : files) {
            try {
                if (addCandidate(FileLoader.load(f.toPath()))) added++;
            } catch (IOException e) {
                showError("Lecture impossible : " + f.getName());
            }
        }
        setStatus(added + " fichier(s) ajouté(s)");
        updateState();
    }

    @FXML
    private void onAddFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Ajouter un dossier de candidats");
        File initial = new File("data");
        if (initial.isDirectory()) dc.setInitialDirectory(initial);
        File dir = dc.showDialog(owner());
        if (dir == null) return;
        try {
            int added = 0;
            for (SourceFile sf : FileLoader.loadFolder(dir.toPath())) {
                if (addCandidate(sf)) added++;
            }
            setStatus(added + " fichier(s) ajouté(s) depuis " + dir.getName());
        } catch (IOException e) {
            showError("Lecture du dossier impossible : " + e.getMessage());
        }
        updateState();
    }

    @FXML
    private void onRemoveSelected() {
        SourceFile sel = candidatesList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            candidatesList.getItems().remove(sel);
            previewArea.clear();
            previewTitle.setText("Aperçu");
        }
        updateState();
    }

    @FXML
    private void onClearCandidates() {
        candidatesList.getItems().clear();
        clearResults();
        updateState();
    }

    @FXML
    private void onClearAll() {
        referenceList.getItems().clear();
        candidatesList.getItems().clear();
        previewArea.clear();
        previewTitle.setText("Aperçu");
        clearResults();
        setStatus("Prêt");
        updateState();
    }

    @FXML
    private void onQuit() {
        Platform.exit();
    }

    // ---------- Menu Affichage ----------

    @FXML private void onThemeLight() { applyTheme(false); }
    @FXML private void onThemeDark()  { applyTheme(true); }

    private void applyTheme(boolean dark) {
        Scene scene = menuBar.getScene();
        String darkCss = getClass().getResource(Constants.CSS_DARK).toExternalForm();
        scene.getStylesheets().remove(darkCss);
        if (dark) scene.getStylesheets().add(darkCss);
    }

    // ---------- Menu Aide ----------

    @FXML
    private void onAbout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("À propos");
        a.setHeaderText("Détecteur de similarité de codes sources");
        a.setContentText("Mini-projet TP algorithmique avancée - Master 1");
        a.initOwner(owner());
        a.showAndWait();
    }

    // ---------- Comparaison ----------

    @FXML
    private void onCompare() {
        if (referenceList.getItems().isEmpty()) {
            showError("Choisissez d'abord un fichier de référence.");
            return;
        }

        Level level;
        switch (algoCombo.getSelectionModel().getSelectedIndex()) {
            case 0:  level = Level.NIVEAU_1; break;
            case 1:  level = Level.NIVEAU_2; break;
            case 2:  level = Level.NIVEAU_3; break;
            default:
                showError("Choisissez un algorithme.");
                return;
        }

        SourceFile reference = referenceList.getItems().get(0);
        List<Row> rows = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (SourceFile cand : candidatesList.getItems()) {
            try {
                double score = SimilarityAlgorithm.similarity(reference, cand, level);
                rows.add(new Row(cand, score, CodeStats.of(cand.getContent(), cand.getName())));
            } catch (IllegalArgumentException ex) {
                skipped.add(cand.getName());
            }
        }

        rows.sort(Comparator.comparingDouble((Row r) -> r.score).reversed());
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).rank = i + 1;
        }

        updateKpis(rows, skipped);
        updateScoreChart(rows);
        resultsTable.getItems().setAll(rows);
        if (!rows.isEmpty()) {
            resultsTable.getSelectionModel().selectFirst();
        } else {
            showDetails(null);
        }
        tabPane.getSelectionModel().select(0);

        setStatus(rows.size() + " comparé(s), " + skipped.size() + " ignoré(s) - "
                + algoCombo.getValue());
    }

    // ---------- Tableau de bord ----------

    private void updateKpis(List<Row> rows, List<String> skipped) {
        kpiCount.setText(String.valueOf(rows.size()));
        kpiSkipped.setText(String.valueOf(skipped.size()));
        kpiSkipped.setTooltip(skipped.isEmpty() ? null : new Tooltip(String.join("\n", skipped)));

        if (rows.isEmpty()) {
            kpiMax.setText("-");
            kpiAvg.setText("-");
            kpiMin.setText("-");
            return;
        }
        double max = rows.get(0).score;
        double min = rows.get(rows.size() - 1).score;
        double sum = 0;
        for (Row r : rows) sum += r.score;
        kpiMax.setText(String.format("%.1f %%", max));
        kpiAvg.setText(String.format("%.1f %%", sum / rows.size()));
        kpiMin.setText(String.format("%.1f %%", min));
    }

    private void updateScoreChart(List<Row> rows) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Row r : rows) {
            XYChart.Data<String, Number> d =
                    new XYChart.Data<>(r.rank + ". " + r.file.getName(), r.score);
            final String style = "-fx-bar-fill: " + color(r.score) + ";";
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle(style);
            });
            series.getData().add(d);
        }
        scoreChart.getData().clear();
        scoreChart.getData().add(series);
    }

    /** Camembert, comparaison en % et mots-clés pour le fichier sélectionné. */
    private void showDetails(Row row) {
        compositionPie.getData().clear();
        compareChart.getData().clear();
        keywordTable.getItems().clear();

        SourceFile ref = referenceList.getItems().isEmpty() ? null : referenceList.getItems().get(0);
        CodeStats refStats = (ref == null) ? null : CodeStats.of(ref.getContent(), ref.getName());
        CodeStats candStats = (row == null) ? null : row.stats;

        CodeStats shown = (candStats != null) ? candStats : refStats;
        String shownName = (candStats != null) ? row.file.getName() : (ref == null ? "" : ref.getName());

        if (shown == null) {
            selectionLabel.setText("Sélectionnez un fichier dans le classement.");
            compositionPie.setTitle("");
            return;
        }

        // texte d'en-tête
        if (candStats != null && refStats != null) {
            selectionLabel.setText("Référence : " + ref.getName() + "   |   Candidat : " + shownName
                    + String.format("   (%.2f %%)", row.score));
        } else {
            selectionLabel.setText("Fichier : " + shownName);
        }

        // camembert : composition du fichier affiché
        compositionPie.setTitle("Composition de " + shownName);
        addSlice("Mots-clés", shown.keywords);
        addSlice("Variables / noms", shown.variables);
        addSlice("Nombres", shown.numbers);
        addSlice("Chaînes", shown.strings);
        addSlice("Symboles", shown.symbols);

        // comparaison en % : référence contre candidat
        if (refStats != null) {
            compareChart.getData().add(percentSeries("Référence", refStats));
        }
        if (candStats != null) {
            compareChart.getData().add(percentSeries("Candidat", candStats));
        }

        // mots-clés les plus utilisés
        Set<String> words = new LinkedHashSet<>();
        if (refStats != null) words.addAll(refStats.keywordCounts.keySet());
        if (candStats != null) words.addAll(candStats.keywordCounts.keySet());

        List<KeywordRow> list = new ArrayList<>();
        for (String w : words) {
            int a = (refStats == null) ? 0 : refStats.keywordCounts.getOrDefault(w, 0);
            int b = (candStats == null) ? 0 : candStats.keywordCounts.getOrDefault(w, 0);
            list.add(new KeywordRow(w, a, b));
        }
        list.sort((x, y) -> Integer.compare(y.ref + y.cand, x.ref + x.cand));
        keywordTable.getItems().setAll(list.size() > 15 ? list.subList(0, 15) : list);
    }

    private void addSlice(String name, int value) {
        if (value > 0) {
            compositionPie.getData().add(new PieChart.Data(name + " (" + value + ")", value));
        }
    }

    private XYChart.Series<String, Number> percentSeries(String name, CodeStats s) {
        double total = Math.max(1, s.tokens);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        series.getData().add(new XYChart.Data<>("Mots-clés", 100.0 * s.keywords / total));
        series.getData().add(new XYChart.Data<>("Variables / noms", 100.0 * s.variables / total));
        series.getData().add(new XYChart.Data<>("Nombres", 100.0 * s.numbers / total));
        series.getData().add(new XYChart.Data<>("Chaînes", 100.0 * s.strings / total));
        series.getData().add(new XYChart.Data<>("Symboles", 100.0 * s.symbols / total));
        return series;
    }

    private void clearResults() {
        resultsTable.getItems().clear();
        scoreChart.getData().clear();
        updateKpis(new ArrayList<>(), new ArrayList<>());
        showDetails(null);
    }

    private static String color(double score) {
        if (score >= HIGH) return "#dc2626";
        if (score >= MEDIUM) return "#f59e0b";
        return "#16a34a";
    }

    private static String levelName(double score) {
        if (score >= HIGH) return "Élevée";
        if (score >= MEDIUM) return "Moyenne";
        return "Faible";
    }

    // ---------- Utilitaires ----------

    private boolean addCandidate(SourceFile sf) {
        boolean exists = candidatesList.getItems().stream()
                .anyMatch(c -> c.getPath().equals(sf.getPath()));
        if (exists) return false;
        candidatesList.getItems().add(sf);
        return true;
    }

    private void showPreview(SourceFile sf) {
        previewTitle.setText(sf.getName() + "  -  " + sf.getLanguage());
        previewArea.setText(sf.getContent());
        tabPane.getSelectionModel().select(previewTab);
    }

    private void updateState() {
        boolean ready = !referenceList.getItems().isEmpty()
                     && !candidatesList.getItems().isEmpty();
        compareButton.setDisable(!ready);
    }

    private FileChooser newFileChooser(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Codes sources",
                "*.java", "*.py", "*.c", "*.h", "*.cpp", "*.hpp", "*.js", "*.dart", "*.cs"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File initial = new File("data");
        if (initial.isDirectory()) fc.setInitialDirectory(initial);
        return fc;
    }

    private Window owner() { return menuBar.getScene().getWindow(); }

    private void setStatus(String msg) { statusLabel.setText(msg); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.initOwner(owner());
        a.showAndWait();
    }
}