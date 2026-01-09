package main;

import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ScriptUI {
    private final Script script;
    private ComboBox<DartType> dartTypeComboBox;
    private Slider speedSlider;
    private Label speedValueLabel;

    public enum DartType {
        BRONZE("Bronze dart tip", 819, 806, 1.8),
        IRON("Iron dart tip", 820, 807, 3.8),
        STEEL("Steel dart tip", 821, 808, 7.5),
        MITHRIL("Mithril dart tip", 822, 810, 11.2),
        ADAMANT("Adamant dart tip", 823, 811, 15.0),
        RUNE("Rune dart tip", 824, 812, 18.8),
        AMETHYST("Amethyst dart tip", 25853, 25849, 21.0),
        DRAGON("Dragon dart tip", 11232, 11230, 25.0),
        ATLATL("Atlatl dart tip", 30998, 30994, 25.0);

        private final String displayName;
        private final int tipID;
        private final int dartID;
        private final double xp;

        DartType(String displayName, int tipID, int dartID, double xp) {
            this.displayName = displayName;
            this.tipID = tipID;
            this.dartID = dartID;
            this.xp = xp;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getTipID() {
            return tipID;
        }

        public int getDartID() {
            return dartID;
        }

        public double getXp() {
            return xp;
        }

        @Override
        public String toString() {
            return displayName + " (" + xp + " XP)";
        }
    }

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(Script script) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #3E3529;");

        Label titleLabel = new Label("Kyyz Dart Maker");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #FFD700;");

        Label versionLabel = new Label("v1.0");
        versionLabel.setStyle("-fx-text-fill: #C9C9C9; -fx-font-size: 10px;");

        VBox settingsBox = new VBox(15);
        settingsBox.setPadding(new Insets(18));
        settingsBox.setStyle(
            "-fx-background-color: #2E2620; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 2; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        Label dartTitle = new Label("Dart Settings");
        dartTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        dartTitle.setStyle("-fx-text-fill: #FF9800; -fx-padding: 0 0 5 0;");

        Label dartTypeLabel = new Label("Dart type:");
        dartTypeLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");

        dartTypeComboBox = new ComboBox<>();
        dartTypeComboBox.getItems().addAll(DartType.values());
        dartTypeComboBox.setValue(DartType.MITHRIL);
        dartTypeComboBox.setMaxWidth(Double.MAX_VALUE);
        dartTypeComboBox.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        dartTypeComboBox.setCellFactory(lv -> new ListCell<DartType>() {
            @Override
            protected void updateItem(DartType dartType, boolean empty) {
                super.updateItem(dartType, empty);
                if (empty || dartType == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(dartType.toString());
                    try {
                        ImageView dartIcon = JavaFXUtils.getItemImageView(script, dartType.getTipID());
                        if (dartIcon != null) {
                            dartIcon.setFitWidth(28);
                            dartIcon.setFitHeight(28);
                            dartIcon.setPreserveRatio(true);
                            dartIcon.setSmooth(false);
                            setGraphic(dartIcon);
                        } else {
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                    setStyle(
                        "-fx-background-color: #1A1510; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-padding: 8 10 8 10; " +
                        "-fx-font-size: 12px;"
                    );
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected && !isEmpty()) {
                    setStyle(
                        "-fx-background-color: #5A4A3A; " +
                        "-fx-text-fill: #FFD700; " +
                        "-fx-padding: 8 10 8 10; " +
                        "-fx-font-size: 12px;"
                    );
                }
            }
        });

        dartTypeComboBox.setButtonCell(new ListCell<DartType>() {
            @Override
            protected void updateItem(DartType dartType, boolean empty) {
                super.updateItem(dartType, empty);
                if (empty || dartType == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(dartType.toString());
                    try {
                        ImageView dartIcon = JavaFXUtils.getItemImageView(script, dartType.getTipID());
                        if (dartIcon != null) {
                            dartIcon.setFitWidth(28);
                            dartIcon.setFitHeight(28);
                            dartIcon.setPreserveRatio(true);
                            dartIcon.setSmooth(false);
                            setGraphic(dartIcon);
                        } else {
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                    setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-padding: 5 8 5 8; " +
                        "-fx-font-size: 12px;"
                    );
                }
            }
        });

        VBox speedSliderBox = new VBox(5);
        HBox speedLabelBox = new HBox();
        speedLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");
        speedValueLabel = new Label("50%");
        speedValueLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region speedSpacer = new Region();
        HBox.setHgrow(speedSpacer, Priority.ALWAYS);
        speedLabelBox.getChildren().addAll(speedLabel, speedSpacer, speedValueLabel);

        speedSlider = new Slider(1, 100, 50);
        speedSlider.setShowTickLabels(false);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(25);
        speedSlider.setMinorTickCount(4);
        speedSlider.setBlockIncrement(5);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            speedValueLabel.setText(newVal.intValue() + "%");
        });

        speedSliderBox.getChildren().addAll(speedLabelBox, speedSlider);

        Label infoLabel = new Label("Note: This script does not bank. Start with tips and feathers in inventory.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle(
            "-fx-text-fill: #FFE55C; " +
            "-fx-font-size: 10px; " +
            "-fx-padding: 10 0 0 0; " +
            "-fx-font-style: italic;"
        );

        settingsBox.getChildren().addAll(
            dartTitle,
            dartTypeLabel,
            dartTypeComboBox,
            speedSliderBox,
            infoLabel
        );

        Button startButton = new Button("START SCRIPT");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setPrefHeight(38);
        startButton.setStyle(
            "-fx-background-color: #FFD700; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-cursor: hand; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 2;"
        );

        startButton.setOnMouseEntered(e ->
            startButton.setStyle(
                "-fx-background-color: #FFA500; " +
                "-fx-text-fill: #000000; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 0; " +
                "-fx-border-radius: 0; " +
                "-fx-cursor: hand; " +
                "-fx-border-color: #5A4A3A; " +
                "-fx-border-width: 2;"
            )
        );

        startButton.setOnMouseExited(e ->
            startButton.setStyle(
                "-fx-background-color: #FFD700; " +
                "-fx-text-fill: #000000; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 0; " +
                "-fx-border-radius: 0; " +
                "-fx-cursor: hand; " +
                "-fx-border-color: #5A4A3A; " +
                "-fx-border-width: 2;"
            )
        );

        startButton.setOnAction(e -> {
            ((javafx.stage.Stage) startButton.getScene().getWindow()).close();
        });

        root.getChildren().addAll(
            titleLabel,
            versionLabel,
            settingsBox,
            startButton
        );

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.web("#3E3529"));
        scene.getRoot().autosize();

        try {
            String css = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
        }

        return scene;
    }

    public int getSelectedDartTipID() {
        DartType selected = dartTypeComboBox.getValue();
        return selected != null ? selected.getTipID() : 822;
    }

    public int getTapSpeed() {
        return (int) speedSlider.getValue();
    }
}
