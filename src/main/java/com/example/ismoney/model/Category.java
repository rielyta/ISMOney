package com.example.ismoney.model;

public class Category {
    private Integer categoriesId;  // Sesuai dengan database (categories_id)
    private String name;
    private String type;
    private String color;

    public Category() {}

    public Category(Integer categoriesId, String name, String type) {
        this.categoriesId = categoriesId;
        this.name = name;
        this.type = type;
        this.color = "#007AFF"; // default color
    }

    public Category(Integer categoriesId, String name, String type, String color) {
        this.categoriesId = categoriesId;
        this.name = name;
        this.type = type;
        this.color = color;
    }

    @Override
    public String toString() {
        return name; // Untuk ComboBox display
    }

    // Getters and Setters
    public Integer getCategoriesId() {
        return categoriesId;
    }

    public void setCategoriesId(Integer categoriesId) {
        this.categoriesId = categoriesId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}