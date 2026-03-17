package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.Tag;
import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.request.TagUpdateRequest;
import com.finance.tracker.exception.DuplicateResourceException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.TagMapper;
import com.finance.tracker.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    private TagServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TagServiceImpl(tagRepository, new TagMapper());
    }

    @Test
    void getTagByIdShouldReturnExistingTag() {
        Tag tag = tag(3L, "food");
        when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));

        assertEquals("food", service.getTagById(3L).getName());
    }

    @Test
    void getTagByIdShouldThrowWhenMissing() {
        when(tagRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTagById(3L));
    }

    @Test
    void getAllTagsShouldReturnMappedTags() {
        when(tagRepository.findAll()).thenReturn(List.of(tag(1L, "food"), tag(2L, "travel")));

        var response = service.getAllTags();

        assertEquals(2, response.size());
        assertEquals("travel", response.get(1).getName());
    }

    @Test
    void createTagShouldNormalizeAndSaveUniqueName() {
        TagRequest request = new TagRequest();
        request.setName("  Travel  ");

        when(tagRepository.existsByName("travel")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(3L);
            return tag;
        });

        var response = service.createTag(request);

        assertEquals(3L, response.getId());
        assertEquals("travel", response.getName());
    }

    @Test
    void createTagShouldThrowWhenNameAlreadyExists() {
        TagRequest request = new TagRequest();
        request.setName("travel");

        when(tagRepository.existsByName("travel")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createTag(request));

        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void updateTagShouldNormalizeNewValue() {
        Tag existing = tag(4L, "food");
        TagUpdateRequest request = new TagUpdateRequest();
        request.setName("  Groceries ");

        when(tagRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByNameAndIdNot("groceries", 4L)).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateTag(4L, request);

        assertEquals("groceries", response.getName());
    }

    @Test
    void updateTagShouldThrowWhenTagIsMissing() {
        when(tagRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateTag(9L, new TagUpdateRequest()));
    }

    @Test
    void updateTagShouldThrowWhenNameAlreadyExists() {
        Tag existing = tag(9L, "food");
        TagUpdateRequest request = new TagUpdateRequest();
        request.setName("travel");

        when(tagRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByNameAndIdNot("travel", 9L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.updateTag(9L, request));
    }

    @Test
    void updateTagShouldKeepCurrentNameWhenRequestNameIsNull() {
        Tag existing = tag(9L, "food");
        TagUpdateRequest request = new TagUpdateRequest();

        when(tagRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateTag(9L, request);

        assertEquals("food", response.getName());
    }

    @Test
    void deleteTagShouldDeleteExistingTag() {
        when(tagRepository.existsById(5L)).thenReturn(true);

        service.deleteTag(5L);

        verify(tagRepository).deleteById(5L);
    }

    @Test
    void deleteTagShouldThrowWhenMissing() {
        when(tagRepository.existsById(5L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteTag(5L));
    }

    private static Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}
