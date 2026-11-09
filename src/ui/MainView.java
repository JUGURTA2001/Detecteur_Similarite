package ui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import algo.SimilarityAlgorithm;
import io.FileLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.SourceFile;
import java.util.ArrayList;
import java.util.Comparator;
import algo.Level;
import util.Constants;

public class MainView {

    @FXML private MenuBar menuBar;
    @FXML private ListView<SourceFile> referenceList;
    @FXML private ListView<SourceFile> candidatesList;
    @FXML private TextArea previewArea;
    @FXML private Label previewTitle;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> algoCombo;
    @FXML private Button compareButton;

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
        updateState();
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
        updateState();
    }

    @FXML
    private void onClearAll() {
        referenceList.getItems().clear();
        candidatesList.getItems().clear();
        previewArea.clear();
        previewTitle.setText("Aperçu");
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

    // ---------- Comparaison (à brancher plus tard) ----------

  

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
                "*.java", "*.py", "*.c", "*.h", "*.cpp", "*.hpp", "*.js", "*.cs"),
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


  // ---------- Comparaison ----------

private static final class Result {
    final SourceFile file;
    final double score;
    Result(SourceFile file, double score) { this.file = file; this.score = score; }
}

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
    default:
        showError("Ce niveau n'est pas encore implémenté.");
        return;
}

    SourceFile reference = referenceList.getItems().get(0);
    List<Result> results = new ArrayList<>();
    List<String> skipped = new ArrayList<>();

    for (SourceFile cand : candidatesList.getItems()) {
        try {
            double score = SimilarityAlgorithm.similarity(reference, cand, level);
            results.add(new Result(cand, score));
        } catch (IllegalArgumentException ex) {
            skipped.add(cand.getName());
        }
    }

    results.sort(Comparator.comparingDouble((Result r) -> r.score).reversed());

    StringBuilder sb = new StringBuilder();
    sb.append("Référence : ").append(reference.getName()).append("\n\n");
    int rank = 1;
    for (Result r : results) {
        sb.append(String.format("%d. %-30s %6.2f %%%n", rank++, r.file.getName(), r.score));
    }
    if (!skipped.isEmpty()) {
        sb.append("\nIgnorés (langage différent ou non supporté) :\n");
        for (String name : skipped) {
            sb.append("  - ").append(name).append("\n");
        }
    }

    previewTitle.setText("Résultats - " + algoCombo.getValue());
    previewArea.setText(sb.toString());
    setStatus(results.size() + " comparé(s), " + skipped.size() + " ignoré(s)");
}



}
