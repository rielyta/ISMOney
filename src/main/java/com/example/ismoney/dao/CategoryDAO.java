package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private DatabaseConfig dbConfig;

    public CategoryDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }


    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT categories_id, name, type, color FROM categories ORDER BY type, name";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Category category = new Category();
                category.setCategoriesId(rs.getInt("categories_id"));
                category.setName(rs.getString("name"));
                category.setType(rs.getString("type"));
                category.setColor(rs.getString("color"));

                categories.add(category);
            }

        } catch (SQLException e) {
            System.err.println("Error getting categories: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    public List<Category> getCategoriesByType(String type) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT categories_id, name, type, color FROM categories WHERE type = ? ORDER BY name";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Category category = new Category();
                category.setCategoriesId(rs.getInt("categories_id"));
                category.setName(rs.getString("name"));
                category.setType(rs.getString("type"));
                category.setColor(rs.getString("color"));

                categories.add(category);
            }

        } catch (SQLException e) {
            System.err.println("Error getting categories by type: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    public Category getCategoryById(int categoryId) {
        String sql = "SELECT categories_id, name, type, color FROM categories WHERE categories_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Category category = new Category();
                category.setCategoriesId(rs.getInt("categories_id"));
                category.setName(rs.getString("name"));
                category.setType(rs.getString("type"));
                category.setColor(rs.getString("color"));

                return category;
            }

        } catch (SQLException e) {
            System.err.println("Error getting category by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}