package com.restaurant.pos.controller;

import com.restaurant.pos.entity.MenuItem;
import com.restaurant.pos.dto.MenuItemRequest;
import com.restaurant.pos.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu-items")
@CrossOrigin(origins = "*")
public class MenuItemController {
    
    @Autowired
    private MenuItemService menuItemService;
    
    @GetMapping
    public ResponseEntity<List<MenuItem>> getAllItems() {
        List<MenuItem> items = menuItemService.getAllItems();
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<MenuItem>> getAvailableItems() {
        List<MenuItem> items = menuItemService.getAllAvailableItems();
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<MenuItem>> getItemsByCategory(@PathVariable String category) {
        try {
            MenuItem.Category cat = MenuItem.Category.valueOf(category);
            List<MenuItem> items = menuItemService.getItemsByCategory(cat);
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getItemById(@PathVariable Long id) {
        return menuItemService.getItemById(id)
            .map(item -> ResponseEntity.ok(item))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody MenuItemRequest request) {
        try {
            MenuItem createdItem = menuItemService.createItem(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest request) {
        try {
            MenuItem updatedItem = menuItemService.updateItem(id, request);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        try {
            menuItemService.deleteItem(id);
            return ResponseEntity.ok(Map.of("message", "Производот е избришан успешно"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PatchMapping("/{id}/toggle-availability")
    public ResponseEntity<?> toggleAvailability(@PathVariable Long id) {
        try {
            MenuItem updatedItem = menuItemService.toggleAvailability(id);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<MenuItem>> searchItems(@RequestParam String q) {
        List<MenuItem> items = menuItemService.searchItems(q);
        return ResponseEntity.ok(items);
    }
}