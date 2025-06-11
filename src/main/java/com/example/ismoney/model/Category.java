package com.example.ismoney.model;

public class Category {
    private String id;
    private String name;
    private String type;

    public Category(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String toString() {
        return name;
    }

    // Getter & Setter...
}

