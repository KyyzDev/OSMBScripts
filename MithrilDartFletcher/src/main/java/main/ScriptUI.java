package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.InputStream;
import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_DART = "selected_dart";

    private final Script script;
    private ComboBox<Integer> dartComboBox;

    // Dart tip item IDs
    private static final int BRONZE_DART_TIP = 819;
    private static final int IRON_DART_TIP = 820;
    private static final int STEEL_DART_TIP = 821;
    private static final int MITHRIL_DART_TIP = 822;
    private static final int ADAMANT_DART_TIP = 823;
    private static final int RUNE_DART_TIP = 824;
    private static final int DRAGON_DART_TIP = 11232;
    private static final int AMETHYST_DART_TIP = 25853;
    private static final int ATLATL_DART_TIPS = 30998;

    // Dropdown options (tips only)
    private static final Integer[] DART_TIP_OPTIONS = {
            BRONZE_DART_TIP,
            IRON_DART_TIP,
            STEEL_DART_TIP,
            MITHRIL_DART_TIP,
            ADAMANT_DART_TIP,
            RUNE_DART_TIP,
            DRAGON_DART_TIP,
            AMETHYST_DART_TIP,
            ATLATL_DART_TIPS
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        // ===== Banner Image (NEW) =====
        ImageView bannerView = createBannerView("/images/dartmaker_banner.png");
        if (bannerView != null) {
            bannerView.setPreserveRatio(true);
            bannerView.setFitHeight(64); // adjust height for your art
            bannerView.setSmooth(true);
            mainBox.getChildren().add(bannerView);
        }

        Label dartLabel = new Label("Choose dart tips to use");
        dartComboBox = createDartComboBox(core);

        // Default to Mithril dart tip if nothing saved yet
        int savedItemId = prefs.getInt(PREF_SELECTED_DART, MITHRIL_DART_TIP);
        boolean matched = false;
        for (Integer option : DART_TIP_OPTIONS) {
            if (option.equals(savedItemId)) {
                dartComboBox.getSelectionModel().select(option);
                matched = true;
                break;
            }
        }
        if (!matched) {
            dartComboBox.getSelectionModel().select(MITHRIL_DART_TIP);
        }

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        mainBox.getChildren().addAll(dartLabel, dartComboBox, confirmButton);

        Scene scene = new Scene(mainBox, 320, 220); // a bit taller for banner
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createDartComboBox(ScriptCore core) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer itemId) {
                return itemId != null ? core.getItemManager().getItemName(itemId) : "";
            }
            @Override
            public Integer fromString(String string) { return null; }
        });

        comboBox.setCellFactory(param -> createItemCell(core));
        comboBox.setButtonCell(createItemCell(core));
        comboBox.getItems().addAll(DART_TIP_OPTIONS);
        return comboBox;
    }

    private ListCell<Integer> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId != null && !empty) {
                    String name = core.getItemManager().getItemName(itemId);
                    ImageView imageView = JavaFXUtils.getItemImageView(core, itemId);
                    if (imageView != null) {
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                    }
                    setGraphic(imageView);
                    setText(name);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        };
    }

    private void saveSettings() {
        Integer selected = dartComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            prefs.putInt(PREF_SELECTED_DART, selected);
            script.log("SAVESETTINGS", "Saved selected dart tip ID: " + selected);
        }
        ((Stage) dartComboBox.getScene().getWindow()).close();
    }

    // ===== Helper to load the banner image =====
    private ImageView createBannerView(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                script.log("WARN", "Banner not found at " + resourcePath);
                return null;
            }
            Image img = new Image(in, 0, 0, true, true);
            return new ImageView(img);
        } catch (Exception e) {
            script.log("WARN", "Failed to load banner: " + e.getMessage());
            return null;
        }
    }

    // Getters
    public int getSelectedDartID() {
        return dartComboBox.getSelectionModel().getSelectedItem();
    }

    // No slider anymore — always run at max speed
    public int getTapSpeed() {
        return 100;
    }
}
