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
    private Slider hpThresholdSlider;
    private Slider foodAmountSlider;
    private ComboBox<LootMode> lootModeComboBox;
    private ComboBox<FoodType> foodComboBox;
    private CheckBox useSeedBoxCheckBox;
    private Label hpValueLabel;
    private Label foodAmountLabel;

    public enum NPCType {
        MAN("Man", 1, 8),
        FARMER("Farmer", 10, 14),
        FEMALE_HAM("Female H.A.M. Member", 15, 18),
        MALE_HAM("Male H.A.M. Member", 20, 22),
        WARRIOR("Warrior", 25, 26),
        ROGUE("Rogue", 32, 36),
        MASTER_FARMER("Master Farmer", 38, 43),
        GUARD("Guard", 40, 47),
        PALADIN("Paladin", 55, 84),
        KNIGHT("Knight of Ardougne", 55, 84),
        HERO("Hero", 80, 275);

        private final String npcName;
        private final int requiredLevel;
        private final int xp;

        NPCType(String npcName, int requiredLevel, int xp) {
            this.npcName = npcName;
            this.requiredLevel = requiredLevel;
            this.xp = xp;
        }

        public String getNpcName() {
            return npcName;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public int getXp() {
            return xp;
        }

        @Override
        public String toString() {
            return npcName + " (Lvl " + requiredLevel + " | " + xp + " XP)";
        }
    }

    public enum LootMode {
        DROP_ALL("Drop All"),
        BANK_WHEN_FULL("Bank When Full");

        private final String displayName;

        LootMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum FoodType {
        NO_FOOD("No Food", 0, -1),
        SHARK("Shark", 20, 385),
        MANTA_RAY("Manta ray", 22, 391),
        DARK_CRAB("Dark crab", 22, 11936),
        SEA_TURTLE("Sea turtle", 21, 397),
        TUNA_POTATO("Tuna potato", 22, 7060),
        KARAMBWAN("Cooked karambwan", 18, 3144),
        LOBSTER("Lobster", 12, 379),
        SWORDFISH("Swordfish", 14, 373),
        MONKFISH("Monkfish", 16, 7946),
        POTATO_WITH_CHEESE("Potato with cheese", 16, 6705),
        TROUT("Trout", 7, 333),
        SALMON("Salmon", 9, 329),
        TUNA("Tuna", 10, 361),
        PIZZA("Pizza", 7, 2289),
        CAKE("Cake", 12, 1891);

        private final String foodName;
        private final int healAmount;
        private final int itemId;

        FoodType(String foodName, int healAmount, int itemId) {
            this.foodName = foodName;
            this.healAmount = healAmount;
            this.itemId = itemId;
        }

        public String getFoodName() {
            return foodName;
        }

        public int getHealAmount() {
            return healAmount;
        }

        public int getItemId() {
            return itemId;
        }

        @Override
        public String toString() {
            if (this == NO_FOOD) {
                return foodName;
            }
            return foodName + " (+" + healAmount + " HP)";
        }
    }

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(Script script) {
        // Root container
        VBox root = new VBox(15);
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #3E3529;"); // OSRS brown background

        // Title
        Label titleLabel = new Label("Kyyz Master Farmer");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #FFD700;"); // Gold color

        Label versionLabel = new Label("v1.0");
        versionLabel.setStyle("-fx-text-fill: #C9C9C9; -fx-font-size: 10px;");

        // === SINGLE SETTINGS BOX ===
        VBox settingsBox = new VBox(15);
        settingsBox.setPadding(new Insets(18));
        settingsBox.setStyle(
            "-fx-background-color: #2E2620; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 2; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        // FOOD SETTINGS
        Label foodTitle = new Label("Food Settings");
        foodTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        foodTitle.setStyle("-fx-text-fill: #FF9800; -fx-padding: 0 0 5 0;"); // OSRS orange

        // Food type dropdown
        Label foodTypeLabel = new Label("Food type:");
        foodTypeLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");

        foodComboBox = new ComboBox<>();
        foodComboBox.getItems().addAll(FoodType.values());
        foodComboBox.setValue(FoodType.SHARK);
        foodComboBox.setMaxWidth(Double.MAX_VALUE);
        foodComboBox.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        // Add custom cell factory to show food icons
        foodComboBox.setCellFactory(lv -> new ListCell<FoodType>() {
            @Override
            protected void updateItem(FoodType food, boolean empty) {
                super.updateItem(food, empty);
                if (empty || food == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(food.toString());
                    if (food == FoodType.NO_FOOD) {
                        // Create custom red X for NO_FOOD option (same size as food icons)
                        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(28, 28);
                        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
                        gc.setStroke(javafx.scene.paint.Color.RED);
                        gc.setLineWidth(3);
                        // Draw X that fills more of the 28x28 space like food icons
                        gc.strokeLine(6, 6, 22, 22);
                        gc.strokeLine(22, 6, 6, 22);
                        setGraphic(canvas);
                    } else {
                        try {
                            ImageView foodIcon = JavaFXUtils.getItemImageView(script, food.getItemId());
                            if (foodIcon != null) {
                                foodIcon.setFitWidth(28);
                                foodIcon.setFitHeight(28);
                                foodIcon.setPreserveRatio(true);
                                foodIcon.setSmooth(false); // Pixel art shouldn't be smoothed
                                setGraphic(foodIcon);
                            } else {
                                setGraphic(null);
                            }
                        } catch (Exception e) {
                            setGraphic(null);
                            script.log("WARN", "Failed to load icon for " + food.getFoodName());
                        }
                    }
                    // Add bottom border separator after NO_FOOD
                    if (food == FoodType.NO_FOOD) {
                        setStyle(
                            "-fx-background-color: #1A1510; " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-padding: 8 10 8 10; " +
                            "-fx-font-size: 12px; " +
                            "-fx-border-color: transparent transparent #3A3A3A transparent; " +
                            "-fx-border-width: 0 0 1 0;"
                        );
                    } else {
                        setStyle(
                            "-fx-background-color: #1A1510; " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-padding: 8 10 8 10; " +
                            "-fx-font-size: 12px;"
                        );
                    }
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

        // Also set button cell (what shows when dropdown is closed)
        foodComboBox.setButtonCell(new ListCell<FoodType>() {
            @Override
            protected void updateItem(FoodType food, boolean empty) {
                super.updateItem(food, empty);
                if (empty || food == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(food.toString());
                    if (food == FoodType.NO_FOOD) {
                        // Create custom red X for NO_FOOD option (same size as food icons)
                        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(28, 28);
                        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
                        gc.setStroke(javafx.scene.paint.Color.RED);
                        gc.setLineWidth(3);
                        // Draw X that fills more of the 28x28 space like food icons
                        gc.strokeLine(6, 6, 22, 22);
                        gc.strokeLine(22, 6, 6, 22);
                        setGraphic(canvas);
                    } else {
                        try {
                            ImageView foodIcon = JavaFXUtils.getItemImageView(script, food.getItemId());
                            if (foodIcon != null) {
                                foodIcon.setFitWidth(28);
                                foodIcon.setFitHeight(28);
                                foodIcon.setPreserveRatio(true);
                                foodIcon.setSmooth(false); // Pixel art shouldn't be smoothed
                                setGraphic(foodIcon);
                            } else {
                                setGraphic(null);
                            }
                        } catch (Exception e) {
                            setGraphic(null);
                            script.log("WARN", "Failed to load icon for " + food.getFoodName());
                        }
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

        // HP Threshold Slider
        VBox hpSliderBox = new VBox(5);
        HBox hpLabelBox = new HBox();
        hpLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label hpLabel = new Label("Eat at HP:");
        hpLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");
        hpValueLabel = new Label("50");
        hpValueLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region hpSpacer = new Region();
        HBox.setHgrow(hpSpacer, Priority.ALWAYS);
        hpLabelBox.getChildren().addAll(hpLabel, hpSpacer, hpValueLabel);

        hpThresholdSlider = new Slider(5, 99, 50);
        hpThresholdSlider.setShowTickLabels(false);
        hpThresholdSlider.setShowTickMarks(true);
        hpThresholdSlider.setMajorTickUnit(20);
        hpThresholdSlider.setMinorTickCount(4);
        hpThresholdSlider.setBlockIncrement(5);
        hpThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            hpValueLabel.setText(String.valueOf(newVal.intValue()));
        });

        hpSliderBox.getChildren().addAll(hpLabelBox, hpThresholdSlider);

        // Food Amount Slider
        VBox foodAmountBox = new VBox(5);
        HBox foodAmountLabelBox = new HBox();
        foodAmountLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label foodAmountTitleLabel = new Label("Food to withdraw:");
        foodAmountTitleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");
        foodAmountLabel = new Label("4");
        foodAmountLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region foodSpacer = new Region();
        HBox.setHgrow(foodSpacer, Priority.ALWAYS);
        foodAmountLabelBox.getChildren().addAll(foodAmountTitleLabel, foodSpacer, foodAmountLabel);

        foodAmountSlider = new Slider(1, 28, 4);
        foodAmountSlider.setShowTickLabels(false);
        foodAmountSlider.setShowTickMarks(true);
        foodAmountSlider.setMajorTickUnit(5);
        foodAmountSlider.setMinorTickCount(4);
        foodAmountSlider.setBlockIncrement(1);
        foodAmountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            foodAmountLabel.setText(String.valueOf(newVal.intValue()));
        });

        foodAmountBox.getChildren().addAll(foodAmountLabelBox, foodAmountSlider);

        // Add listener to hide/show sliders when NO_FOOD is selected
        foodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == FoodType.NO_FOOD) {
                // Hide both sliders when NO_FOOD is selected
                hpSliderBox.setVisible(false);
                hpSliderBox.setManaged(false);
                foodAmountBox.setVisible(false);
                foodAmountBox.setManaged(false);
            } else {
                // Show sliders when any actual food is selected
                hpSliderBox.setVisible(true);
                hpSliderBox.setManaged(true);
                foodAmountBox.setVisible(true);
                foodAmountBox.setManaged(true);
            }
        });

        // LOOT MODE - Dropdown
        Label lootTitle = new Label("Loot mode:");
        lootTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-padding: 10 0 0 0;");

        lootModeComboBox = new ComboBox<>();
        lootModeComboBox.getItems().addAll(LootMode.values());
        lootModeComboBox.setValue(LootMode.DROP_ALL);
        lootModeComboBox.setMaxWidth(Double.MAX_VALUE);
        lootModeComboBox.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        // Add custom cell factory for loot mode dropdown (same style as food)
        lootModeComboBox.setCellFactory(lv -> new ListCell<LootMode>() {
            @Override
            protected void updateItem(LootMode lootMode, boolean empty) {
                super.updateItem(lootMode, empty);
                if (empty || lootMode == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(lootMode.toString());
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

        // Also set button cell (what shows when dropdown is closed)
        lootModeComboBox.setButtonCell(new ListCell<LootMode>() {
            @Override
            protected void updateItem(LootMode lootMode, boolean empty) {
                super.updateItem(lootMode, empty);
                if (empty || lootMode == null) {
                    setText(null);
                } else {
                    setText(lootMode.toString());
                    setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-padding: 5 8 5 8; " +
                        "-fx-font-size: 12px;"
                    );
                }
            }
        });

        // SEED BOX - Premium styled container with icon
        HBox seedBoxContainer = new HBox(12);
        seedBoxContainer.setAlignment(Pos.CENTER);
        seedBoxContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2A2218 0%, #1A1510 100%); " +
            "-fx-border-color: linear-gradient(to bottom, #8B7355 0%, #5A4A3A 100%); " +
            "-fx-border-width: 2; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-padding: 12 16 12 16; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 4, 0, 0, 2);"
        );

        // Add seed box icon with glow effect
        try {
            ImageView seedBoxIcon = JavaFXUtils.getItemImageView(script, 13639); // Seed box ID
            if (seedBoxIcon != null) {
                seedBoxIcon.setFitWidth(36);
                seedBoxIcon.setFitHeight(36);
                seedBoxIcon.setPreserveRatio(true);
                seedBoxIcon.setSmooth(false); // Pixel art
                seedBoxIcon.setStyle("-fx-effect: dropshadow(gaussian, rgba(255, 215, 0, 0.6), 8, 0.5, 0, 0);");
                seedBoxContainer.getChildren().add(seedBoxIcon);
            }
        } catch (Exception e) {
            script.log("WARN", "Failed to load seed box icon");
        }

        useSeedBoxCheckBox = new CheckBox("Use Seed Box");
        useSeedBoxCheckBox.setSelected(false);
        useSeedBoxCheckBox.setStyle(
            "-fx-text-fill: #FFE55C; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold;"
        );

        seedBoxContainer.getChildren().add(useSeedBoxCheckBox);

        settingsBox.getChildren().addAll(
            foodTitle,
            foodTypeLabel,
            foodComboBox,
            hpSliderBox,
            foodAmountBox,
            lootTitle,
            lootModeComboBox,
            seedBoxContainer
        );

        // Start button
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

        // Auto-size the scene with brown background
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.web("#3E3529")); // Set scene background to brown
        scene.getRoot().autosize();

        // Load the OSRS-style CSS
        try {
            String css = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            script.log("INFO", "Loaded custom CSS stylesheet");
        } catch (Exception e) {
            script.log("WARN", "Could not load CSS stylesheet: " + e.getMessage());
        }

        return scene;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean shouldEatFood() {
        return true; // Always eat food (checkbox removed from UI)
    }

    public int getHpThreshold() {
        return (int) hpThresholdSlider.getValue();
    }

    public int getFoodAmount() {
        return (int) foodAmountSlider.getValue();
    }

    public LootMode getLootMode() {
        return lootModeComboBox.getValue();
    }

    public FoodType getFoodType() {
        return foodComboBox.getValue();
    }

    public boolean useSeedBox() {
        return useSeedBoxCheckBox.isSelected();
    }
}
