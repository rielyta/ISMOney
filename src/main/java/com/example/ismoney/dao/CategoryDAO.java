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

    // CREATE
    public boolean addCategory(Category category) {
        String sql = "INSERT INTO categories (name, type, color) VALUES (?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, category.getName());
            stmt.setString(2, category.getType());
            stmt.setString(3, category.getColor());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    category.setCategoriesId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // READ
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

    // UPDATE
    public boolean updateCategory(Category category) {
        String sql = "UPDATE categories SET name = ?, type = ?, color = ? WHERE categories_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.getName());
            stmt.setString(2, category.getType());
            stmt.setString(3, category.getColor());
            stmt.setInt(4, category.getCategoriesId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // DELETE
    public boolean deleteCategory(int categoryId) {
        String sql = "DELETE FROM categories WHERE categories_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}