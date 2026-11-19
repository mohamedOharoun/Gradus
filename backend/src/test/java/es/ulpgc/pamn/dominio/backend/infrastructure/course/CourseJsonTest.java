package es.ulpgc.pamn.dominio.backend.infrastructure.course;

import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseIssueCode;
import es.ulpgc.pamn.dominio.backend.domain.course.Issue;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static es.ulpgc.pamn.dominio.backend.infrastructure.course.Utf8TextTest.BOM;
import static es.ulpgc.pamn.dominio.backend.infrastructure.course.Utf8TextTest.concat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseJsonTest {

    static final CourseJson JSON = new CourseJson();

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static Issue failure(SyntaxCheck check) {
        return assertInstanceOf(SyntaxCheck.Failed.class, check).issue();
    }

    static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @Test
    void theExampleIsParsedKeepingItsTextAndTheHashOfItsBytes() throws NoSuchAlgorithmException {
        SyntaxCheck.Passed passed = assertInstanceOf(SyntaxCheck.Passed.class, JSON.parse(ExampleCourse.bytes()));

        assertEquals(ExampleCourse.text(), passed.content().text());
        assertEquals(sha256(ExampleCourse.bytes()), passed.content().sha256());
        assertEquals("Introducción a los grafos", passed.tree().get("title").asText());
    }

    @Test
    void aLeadingBomIsAcceptedAndLeftOutOfTheContentAndItsHash() throws NoSuchAlgorithmException {
        SyntaxCheck.Passed passed = assertInstanceOf(SyntaxCheck.Passed.class,
                JSON.parse(concat(BOM, ExampleCourse.bytes())));

        assertFalse(passed.content().text().startsWith("﻿"));
        assertEquals(sha256(ExampleCourse.bytes()), passed.content().sha256());
    }

    @Test
    void invalidUtf8IsInvalidJsonAtTheRoot() {
        byte[] body = ExampleCourse.bytes();
        body[10] = (byte) 0xFF;

        Issue issue = failure(JSON.parse(body));

        assertEquals(CourseIssueCode.INVALID_JSON, issue.code());
        assertEquals("", issue.path().toString());
        assertTrue(issue.message().contains("UTF-8") && issue.message().contains("10"), issue.message());
    }

    @Test
    void brokenJsonIsReportedWithLineAndColumn() {
        String broken = ExampleCourse.text().replaceFirst("\"title\":", "\"title\"");

        Issue issue = failure(JSON.parse(utf8(broken)));

        assertEquals(CourseIssueCode.INVALID_JSON, issue.code());
        assertTrue(issue.message().contains("línea 3"), issue.message());
        assertTrue(issue.message().contains("columna"), issue.message());
    }

    @Test
    void aRepeatedKeyIsReportedOnTheObjectThatRepeatsIt() {
        // §6: «"answer": "a", "answer": "b"» da DUPLICATE_KEY. El primer answer del ejemplo es el de repr-f1.
        String repeated = ExampleCourse.text().replaceFirst("\"answer\": \"a\",", "\"answer\": \"a\", \"answer\": \"b\",");

        Issue issue = failure(JSON.parse(utf8(repeated)));

        assertEquals(CourseIssueCode.DUPLICATE_KEY, issue.code());
        assertEquals("/modules/0/steps/2", issue.path().toString());
        assertTrue(issue.message().contains("«answer»"), issue.message());
    }

    @Test
    void aRepeatedKeyAtTheTopIsReportedAtTheRoot() {
        Issue issue = failure(JSON.parse(utf8("{\"title\": \"A\", \"title\": \"B\"}")));

        assertEquals(CourseIssueCode.DUPLICATE_KEY, issue.code());
        assertEquals("", issue.path().toString());
    }

    @Test
    void anythingAfterTheDocumentIsInvalidJson() {
        Issue issue = failure(JSON.parse(utf8(ExampleCourse.text() + "\n{}")));

        assertEquals(CourseIssueCode.INVALID_JSON, issue.code());
    }

    @Test
    void anEmptyOrBlankBodyIsInvalidJson() {
        assertEquals(CourseIssueCode.INVALID_JSON, failure(JSON.parse(new byte[0])).code());
        assertEquals(CourseIssueCode.INVALID_JSON, failure(JSON.parse(utf8(" \n "))).code());
    }

    @Test
    void aBodyOverTheLimitIsTooLarge() {
        byte[] huge = utf8("\"" + "x".repeat(CourseContent.MAX_SIZE_BYTES) + "\"");

        Issue issue = failure(JSON.parse(huge));

        assertEquals(CourseIssueCode.PAYLOAD_TOO_LARGE, issue.code());
        assertTrue(issue.message().contains("2 MiB"), issue.message());
    }
}
