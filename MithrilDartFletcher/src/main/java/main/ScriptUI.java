package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_DART = "selected_dart";

    private final Script script;
    private ComboBox<Integer> dartComboBox;

    private static final Integer[] DART_OPTIONS = {
            ItemID.MITHRIL_DART
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label dartLabel = new Label("Choose Darts to make");
        dartComboBox = createDartComboBox(core);

        int savedItemId = prefs.getInt(PREF_SELECTED_DART, ItemID.MITHRIL_DART);
        for (Integer option : DART_OPTIONS) {
            if (option.equals(savedItemId)) {
                dartComboBox.getSelectionModel().select(option);
                break;
            }
        }

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        // Only the picker + confirm, no slider
        mainBox.getChildren().addAll(dartLabel, dartComboBox, confirmButton);

        Scene scene = new Scene(mainBox, 300, 180);
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
            public Integer fromString(String string) {
                return null;
            }
        });

        comboBox.setCellFactory(param -> createItemCell(core));
        comboBox.setButtonCell(createItemCell(core));
        comboBox.getItems().addAll(DART_OPTIONS);
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
            script.log("SAVESETTINGS", "Saved selected dart ID: " + selected);
        }

        ((Stage) dartComboBox.getScene().getWindow()).close();
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
