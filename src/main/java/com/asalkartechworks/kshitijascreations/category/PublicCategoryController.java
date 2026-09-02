package com.asalkartechworks.kshitijascreations.category;

import com.asalkartechworks.kshitijascreations.category.dto.PublicCategoryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/categories")
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<PublicCategoryResponse> list() {
        return categoryService.publicList();
    }
}
