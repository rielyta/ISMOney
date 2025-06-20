package com.example.ismoney.model;

import javafx.beans.property.*;

public class Category {
    private IntegerProperty categoriesId;
    private StringProperty name;
    private StringProperty type;
    private StringProperty color;

    public Category() {
        this.categoriesId = new SimpleIntegerProperty();
        this.name = new SimpleStringProperty();
        this.type = new SimpleStringProperty();
        this.color = new SimpleStringProperty("#007AFF");
    }

    public Category(Integer categoriesId, String name, String type) {
        this();
        setCategoriesId(categoriesId);
        setName(name);
        setType(type);
    }

    public Category(Integer categoriesId, String name, String type, String color) {
        this();
        setCategoriesId(categoriesId);
        setName(name);
        setType(type);
        setColor(color);
    }

    @Override
    public String toString() {
        return getName(); // Untuk ComboBox display
    }

    // Property getters untuk JavaFX binding
    public IntegerProperty categoriesIdProperty() { return categoriesId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty typeProperty() { return type; }
    public StringProperty colorProperty() { return color; }

    // Traditional getters and setters
    public Integer getCategoriesId() {
        return categoriesId.get();
    }

    public void setCategoriesId(Integer categoriesId) {
        this.categoriesId.set(categoriesId);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public String getColor() {
        return color.get();
    }

    public void setColor(String color) {
        this.color.set(color);
    }

    // Utility methods
    public boolean isIncomeCategory() {
        return "INCOME".equals(getType());
    }

    public boolean isOutcomeCategory() {
        return "OUTCOME".equals(getType());
    }
}