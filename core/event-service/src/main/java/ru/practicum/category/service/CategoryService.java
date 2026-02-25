package ru.practicum.category.service;

import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.dto.event.CategoryDto;

import java.util.List;

public interface CategoryService {

    ru.practicum.dto.event.CategoryDto addCategory(NewCategoryDto newCategory);

    void deleteCategory(Long catId);

    ru.practicum.dto.event.CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    List<CategoryDto> getCategories(int from, int size);

    ru.practicum.dto.event.CategoryDto getCategory(Long catId);

}