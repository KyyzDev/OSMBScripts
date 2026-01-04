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

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    private static final String PREF_WEBHOOK_ENABLED = "kyyz_masterfarmer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "kyyz_masterfarmer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "kyyz_masterfarmer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "kyyz_masterfarmer_webhook_include_user";

    private final Script script;
    private ComboBox<Location> locationComboBox;
    private Slider hpThresholdSlider;
    private Slider foodAmountSlider;
    private ComboBox<LootMode> lootModeComboBox;
    private ComboBox<FoodType> foodComboBox;
    private CheckBox useSeedBoxCheckBox;
    private Label hpValueLabel;
    private Label foodAmountLabel;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

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

    public enum Location {
        DRAYNOR("Draynor Village", 3079, 3250, 0, 3081, 3249, 12338);

        private final String displayName;
        private final int farmerX, farmerY, farmerZ;
        private final int centerX, centerY;
        private final int regionId;

        Location(String displayName, int farmerX, int farmerY, int farmerZ, int centerX, int centerY, int regionId) {
            this.displayName = displayName;
            this.farmerX = farmerX;
            this.farmerY = farmerY;
            this.farmerZ = farmerZ;
            this.centerX = centerX;
            this.centerY = centerY;
            this.regionId = regionId;
        }

        public String getDisplayName() { return displayName; }
        public int getFarmerX() { return farmerX; }
        public int getFarmerY() { return farmerY; }
        public int getFarmerZ() { return farmerZ; }
        public int getCenterX() { return centerX; }
        public int getCenterY() { return centerY; }
        public int getRegionId() { return regionId; }

        @Override
        public String toString() { return displayName; }
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
        VBox root = new VBox(15);
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #3E3529;"); 

        
        Label titleLabel = new Label("Kyyz Master Farmer");
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

        Label locationTitle = new Label("Location");
        locationTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        locationTitle.setStyle("-fx-text-fill: #FF9800; -fx-padding: 0 0 5 0;");

        locationComboBox = new ComboBox<>();
        locationComboBox.getItems().addAll(Location.values());
        locationComboBox.setValue(Location.DRAYNOR);
        locationComboBox.setMaxWidth(Double.MAX_VALUE);
        locationComboBox.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        locationComboBox.setCellFactory(lv -> new ListCell<Location>() {
            @Override
            protected void updateItem(Location loc, boolean empty) {
                super.updateItem(loc, empty);
                if (empty || loc == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(loc.toString());
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

        locationComboBox.setButtonCell(new ListCell<Location>() {
            @Override
            protected void updateItem(Location loc, boolean empty) {
                super.updateItem(loc, empty);
                if (empty || loc == null) {
                    setText(null);
                } else {
                    setText(loc.toString());
                    setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-padding: 5 8 5 8; " +
                        "-fx-font-size: 12px;"
                    );
                }
            }
        });

        Label foodTitle = new Label("Food Settings");
        foodTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        foodTitle.setStyle("-fx-text-fill: #FF9800; -fx-padding: 10 0 5 0;");

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
                        
                        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(28, 28);
                        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
                        gc.setStroke(javafx.scene.paint.Color.RED);
                        gc.setLineWidth(3);
                        
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
                                foodIcon.setSmooth(false); 
                                setGraphic(foodIcon);
                            } else {
                                setGraphic(null);
                            }
                        } catch (Exception e) {
                            setGraphic(null);
                            script.log("WARN", "Failed to load icon for " + food.getFoodName());
                        }
                    }
                    
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
                        
                        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(28, 28);
                        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
                        gc.setStroke(javafx.scene.paint.Color.RED);
                        gc.setLineWidth(3);
                        
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
                                foodIcon.setSmooth(false); 
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

        hpThresholdSlider = new Slider(10, 99, 50);
        hpThresholdSlider.setShowTickLabels(false);
        hpThresholdSlider.setShowTickMarks(true);
        hpThresholdSlider.setMajorTickUnit(20);
        hpThresholdSlider.setMinorTickCount(4);
        hpThresholdSlider.setBlockIncrement(5);
        hpThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            hpValueLabel.setText(String.valueOf(newVal.intValue()) + " HP");
        });

        hpSliderBox.getChildren().addAll(hpLabelBox, hpThresholdSlider);

        
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

        
        foodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == FoodType.NO_FOOD) {
                
                hpSliderBox.setVisible(false);
                hpSliderBox.setManaged(false);
                foodAmountBox.setVisible(false);
                foodAmountBox.setManaged(false);
            } else {
                
                hpSliderBox.setVisible(true);
                hpSliderBox.setManaged(true);
                foodAmountBox.setVisible(true);
                foodAmountBox.setManaged(true);
            }
        });

        
        Label lootTitle = new Label("Loot mode:");
        lootTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-padding: 10 0 0 0;");

        lootModeComboBox = new ComboBox<>();
        lootModeComboBox.getItems().addAll(LootMode.values());
        lootModeComboBox.setValue(LootMode.BANK_WHEN_FULL);
        lootModeComboBox.setMaxWidth(Double.MAX_VALUE);
        lootModeComboBox.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        
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



        try {
            ImageView seedBoxIcon = JavaFXUtils.getItemImageView(script, KyyzMasterFarmer.SEED_BOX_EMPTY); 
            if (seedBoxIcon != null) {
                seedBoxIcon.setFitWidth(36);
                seedBoxIcon.setFitHeight(36);
                seedBoxIcon.setPreserveRatio(true);
                seedBoxIcon.setSmooth(false); 
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

        Label webhookTitle = new Label("Webhook Settings");
        webhookTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        webhookTitle.setStyle("-fx-text-fill: #FF9800; -fx-padding: 10 0 5 0;");

        webhookEnabledCheckBox = new CheckBox("Enable Discord Webhook");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));
        webhookEnabledCheckBox.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");

        webhookUrlField = new TextField();
        webhookUrlField.setPromptText("Discord Webhook URL...");
        webhookUrlField.setText(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());
        webhookUrlField.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        HBox intervalBox = new HBox(10);
        intervalBox.setAlignment(Pos.CENTER_LEFT);
        Label intervalLabel = new Label("Interval (min):");
        intervalLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());
        webhookIntervalComboBox.setMaxWidth(100);
        webhookIntervalComboBox.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-background-color: #1A1510; " +
            "-fx-border-color: #5A4A3A; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 0; " +
            "-fx-border-radius: 0;"
        );

        includeUsernameCheckBox = new CheckBox("Include Username");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));
        includeUsernameCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());
        includeUsernameCheckBox.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        intervalBox.getChildren().addAll(intervalLabel, webhookIntervalComboBox, spacer, includeUsernameCheckBox);

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
        });

        settingsBox.getChildren().addAll(
            locationTitle,
            locationComboBox,
            foodTitle,
            foodTypeLabel,
            foodComboBox,
            hpSliderBox,
            foodAmountBox,
            lootTitle,
            lootModeComboBox,
            seedBoxContainer,
            webhookTitle,
            webhookEnabledCheckBox,
            webhookUrlField,
            intervalBox
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
            saveSettings();
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
            script.log("INFO", "Loaded custom CSS stylesheet");
        } catch (Exception e) {
            script.log("WARN", "Could not load CSS stylesheet: " + e.getMessage());
        }

        return scene;
    }

    private void saveSettings() {
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean shouldEatFood() {
        return true; 
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

    public boolean isWebhookEnabled() {
        return webhookEnabledCheckBox != null && webhookEnabledCheckBox.isSelected();
    }

    public String getWebhookUrl() {
        return webhookUrlField != null ? webhookUrlField.getText().trim() : "";
    }

    public int getWebhookInterval() {
        return webhookIntervalComboBox != null && webhookIntervalComboBox.getValue() != null
                ? webhookIntervalComboBox.getValue()
                : 5;
    }

    public boolean isUsernameIncluded() {
        return includeUsernameCheckBox != null && includeUsernameCheckBox.isSelected();
    }

    public Location getLocation() {
        return locationComboBox != null ? locationComboBox.getValue() : Location.DRAYNOR;
    }
}
