package com.finance.tracker.mapper;

import com.finance.tracker.domain.Tag;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.TagRequest;
import com.finance.tracker.dto.response.TagResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        if (tag == null) {
            return null;
        }

        TagResponse response = new TagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setUserId(tag.getUser() != null ? tag.getUser().getId() : null);
        return response;
    }

    public Tag fromRequest(TagRequest request, User user) {
        if (request == null) {
            return null;
        }

        Tag tag = new Tag();
        tag.setName(request.getName().trim());
        tag.setNormalizedName(normalizeName(request.getName()));
        tag.setUser(user);
        return tag;
    }

    public String normalizeName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }
}
