package com.ai.ticketingApp.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentModelTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void constructorRetainsFields() {
        CommentModel comment = new CommentModel("qa-user", "Verified the fix", 1234L);

        assertEquals("qa-user", comment.getAuthor());
        assertEquals("Verified the fix", comment.getBody());
        assertEquals(1234L, comment.getCreated());
    }

    @Test
    void fromJsonDeserializesComment() throws Exception {
        String json = "{\"author\":\"dev-user\",\"body\":\"Fixed in PR 42\",\"created\":9999}";

        CommentModel comment = CommentModel.fromJson(json, MAPPER);

        assertEquals("dev-user", comment.getAuthor());
        assertEquals("Fixed in PR 42", comment.getBody());
        assertEquals(9999L, comment.getCreated());
    }
}
