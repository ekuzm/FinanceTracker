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
import com.finance.tracker.mapper.TagMapper;
import com.finance.tracker.repository.TagRepository;
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
        Tag existing = new Tag();
        existing.setId(4L);
        existing.setName("food");

        TagUpdateRequest request = new TagUpdateRequest();
        request.setName("  Groceries ");

        when(tagRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByNameAndIdNot("groceries", 4L)).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateTag(4L, request);

        assertEquals("groceries", response.getName());
    }
}
