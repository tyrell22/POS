package com.restaurant.pos.service;

import com.restaurant.pos.entity.MenuItem;
import com.restaurant.pos.dto.MenuItemRequest;
import com.restaurant.pos.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MenuItemService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    public List<MenuItem> getAllAvailableItems() {
        return menuItemRepository.findByAvailableTrueOrderByNameAsc();
    }

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findAllByOrderByNameAsc();
    }

    public List<MenuItem> getItemsByCategory(MenuItem.Category category) {
        return menuItemRepository.findByCategoryAndAvailableTrueOrderByNameAsc(category);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public MenuItem createItem(MenuItemRequest request) {
        MenuItem menuItem = new MenuItem();
        menuItem.setName(request.getName());
        menuItem.setPrice(request.getPrice());
        menuItem.setCategory(MenuItem.Category.valueOf(request.getCategory()));
        menuItem.setPrintDestination(MenuItem.PrintDestination.valueOf(request.getPrintDestination()));
        menuItem.setAvailable(true);

        return menuItemRepository.save(menuItem);
    }

    public MenuItem updateItem(Long id, MenuItemRequest request) {
        MenuItem menuItem = menuItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

        menuItem.setName(request.getName());
        menuItem.setPrice(request.getPrice());
        menuItem.setCategory(MenuItem.Category.valueOf(request.getCategory()));
        menuItem.setPrintDestination(MenuItem.PrintDestination.valueOf(request.getPrintDestination()));

        return menuItemRepository.save(menuItem);
    }

    public void deleteItem(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new RuntimeException("Производот не е пронајден");
        }
        menuItemRepository.deleteById(id);
    }

    public MenuItem toggleAvailability(Long id) {
        MenuItem menuItem = menuItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Производот не е пронајден"));

        menuItem.setAvailable(!menuItem.getAvailable());
        return menuItemRepository.save(menuItem);
    }

    public List<MenuItem> searchItems(String searchTerm) {
        return menuItemRepository.findByNameContainingIgnoreCaseAndAvailableTrueOrderByNameAsc(searchTerm);
    }
}