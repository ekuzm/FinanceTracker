package com.finance.tracker.service.impl;

import com.finance.tracker.domain.Tag;
import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.response.TagResponse;
import com.finance.tracker.mapper.TagMapper;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.service.TagService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public TagResponse getTagById(Long id) {
        return executeWithLogging("getTagById", () -> {
            Tag tag = tagRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found " + id));
            return tagMapper.toResponse(tag);
        });
    }

    @Override
    public List<TagResponse> getAllTags() {
        return executeWithLogging("getAllTags", () -> tagRepository.findAll().stream()
                .map(tagMapper::toResponse)
                .toList());
    }

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        return executeWithLogging("createTag", () -> {
            String normalizedName = tagMapper.normalizeName(request.getName());
            if (tagRepository.existsByName(normalizedName)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tag with name '" + request.getName() + "' already exists");
            }
            Tag tag = tagMapper.fromRequest(request);
            Tag saved = tagRepository.save(tag);
            return tagMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        return executeWithLogging("updateTag", () -> {
            Tag tag = tagRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found " + id));
            if (request.getName() != null) {
                String normalizedName = tagMapper.normalizeName(request.getName());
                if (tagRepository.existsByNameAndIdNot(normalizedName, id)) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Tag with name '" + request.getName() + "' already exists");
                }
                tag.setName(normalizedName);
            }
            Tag saved = tagRepository.save(tag);
            return tagMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        executeWithLogging("deleteTag", () -> {
            if (!tagRepository.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found " + id);
            }
            tagRepository.deleteById(id);
        });
    }

    private <T> T executeWithLogging(String methodName, java.util.function.Supplier<T> action) {
        long startTime = System.currentTimeMillis();
        try {
            T result = action.get();
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug("Method TagServiceImpl.{} completed in {} ms", methodName, executionTimeMs);
            return result;
        } catch (RuntimeException exception) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug(
                    "Method TagServiceImpl.{} failed in {} ms: {}",
                    methodName,
                    executionTimeMs,
                    exception.getMessage());
            throw exception;
        }
    }

    private void executeWithLogging(String methodName, Runnable action) {
        long startTime = System.currentTimeMillis();
        try {
            action.run();
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug("Method TagServiceImpl.{} completed in {} ms", methodName, executionTimeMs);
        } catch (RuntimeException exception) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            log.debug(
                    "Method TagServiceImpl.{} failed in {} ms: {}",
                    methodName,
                    executionTimeMs,
                    exception.getMessage());
            throw exception;
        }
    }
}
