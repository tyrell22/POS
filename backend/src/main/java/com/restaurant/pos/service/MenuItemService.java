package com.restaurant.pos.service;

import com.restaurant.pos.entity.MenuItem;
import com.restaurant.pos.dto.MenuItemRequest;
import com.restaurant.pos.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MenuItemService {

    private static final Logger logger = LoggerFactory.getLogger(MenuItemService.class);

    @Autowired
    private MenuItemRepository menuItemRepository;
    
    @Autowired
    private DatabaseHealthService databaseHealthService;

    @Transactional(readOnly = true)
    public List<MenuItem> getAllAvailableItems() {
        try {
            return menuItemRepository.findByAvailableTrueOrderByNameAsc();
        } catch (DataAccessException e) {
            logger.error("Database error getting available items", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getAllItems() {
        try {
            return menuItemRepository.findAllByOrderByNameAsc();
        } catch (DataAccessException e) {
            logger.error("Database error getting all items", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getItemsByCategory(MenuItem.Category category) {
        try {
            return menuItemRepository.findByCategoryAndAvailableTrueOrderByNameAsc(category);
        } catch (DataAccessException e) {
            logger.error("Database error getting items by category", e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    @Transactional(readOnly = true)
    public Optional<MenuItem> getItemById(Long id) {
        try {
            if (id == null) {
                return Optional.empty();
            }
            return menuItemRepository.findById(id);
        } catch (DataAccessException e) {
            logger.error("Database error getting item by ID: {}", id, e);
            throw new RuntimeException("Грешка при пристап до базата на податоци");
        }
    }

    public MenuItem createItem(MenuItemRequest request) {
        try {
            // Check database health before operation
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("Базата на податоци не е достапна");
            }
            
            MenuItem menuItem = new MenuItem();
            menuItem.setName(request.getName());
            menuItem.setPrice(request.getPrice());
            menuItem.setCategory(MenuItem.Category.valueOf(request.getCategory()));
            menuItem.setPrintDestination(MenuItem.PrintDestination.valueOf(request.getPrintDestination()));
            menuItem.setAvailable(true);

            MenuItem savedItem = menuItemRepository.save(menuItem);
            logger.info("Created menu item: {} with ID: {}", savedItem.getName(), savedItem.getId());
            return savedItem;
        } catch (DataAccessException e) {
            logger.error("Database error creating item", e);
            throw new RuntimeException("Грешка при креирање на производот");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid category or print destination", e);
            throw new RuntimeException("Неважечка категорија или дестинација");
        }
    }

    public MenuItem updateItem(Long id, MenuItemRequest request) {
        try {
            // Check database health before operation
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("Базата на податоци не е достапна");
            }
            
            MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

            menuItem.setName(request.getName());
            menuItem.setPrice(request.getPrice());
            menuItem.setCategory(MenuItem.Category.valueOf(request.getCategory()));
            menuItem.setPrintDestination(MenuItem.PrintDestination.valueOf(request.getPrintDestination()));

            MenuItem updatedItem = menuItemRepository.save(menuItem);
            logger.info("Updated menu item: {} with ID: {}", updatedItem.getName(), updatedItem.getId());
            return updatedItem;
        } catch (DataAccessException e) {
            logger.error("Database error updating item with ID: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на производот");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid category or print destination for item: {}", id, e);
            throw new RuntimeException("Неважечка категорија или дестинација");
        }
    }

    public void deleteItem(Long id) {
        try {
            // Check database health before operation
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("Базата на податоци не е достапна");
            }
            
            // Check if item exists first
            if (!menuItemRepository.existsById(id)) {
                logger.warn("Attempt to delete non-existent menu item with ID: {}", id);
                throw new RuntimeException("Производот не е пронајден");
            }
            
            menuItemRepository.deleteById(id);
            logger.info("Deleted menu item with ID: {}", id);
        } catch (DataAccessException e) {
            logger.error("Database error deleting item with ID: {}", id, e);
            throw new RuntimeException("Грешка при бришење на производот");
        }
    }

    public MenuItem toggleAvailability(Long id) {
        try {
            // Check database health before operation
            if (!databaseHealthService.isDatabaseHealthy()) {
                throw new RuntimeException("Базата на податоци не е достапна");
            }
            
            MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

            menuItem.setAvailable(!menuItem.getAvailable());
            MenuItem updatedItem = menuItemRepository.save(menuItem);
            
            logger.info("Toggled availability for menu item: {} (ID: {}) to: {}", 
                updatedItem.getName(), updatedItem.getId(), updatedItem.getAvailable());
            return updatedItem;
        } catch (DataAccessException e) {
            logger.error("Database error toggling availability for item with ID: {}", id, e);
            throw new RuntimeException("Грешка при ажурирање на достапноста");
        }
    }

    @Transactional(readOnly = true)
    public List<MenuItem> searchItems(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllAvailableItems();
            }
            return menuItemRepository.findByNameContainingIgnoreCaseAndAvailableTrueOrderByNameAsc(searchTerm.trim());
        } catch (DataAccessException e) {
            logger.error("Database error searching items with term: {}", searchTerm, e);
            throw new RuntimeException("Грешка при пребарување на производи");
        }
    }
}