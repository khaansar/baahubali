package com.example.attemptservice.service;

import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.entity.AttemptAnswer;
import com.example.attemptservice.event.AttemptSubmittedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class AttemptAnalyticsEventFactory {

    private AttemptAnalyticsEventFactory() {
    }

    public static AttemptSubmittedEvent create(
            String eventId,
            String attemptId,
            String userId,
            InternalTestBlueprintDto blueprint,
            List<AttemptAnswer> answers,
            Long timeTakenSeconds,
            Instant timestamp
    ) {
        Map<String, String> answerMap = new HashMap<>();

        for (AttemptAnswer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer.getSelectedOption());
        }

        return new AttemptSubmittedEvent(
                eventId,
                attemptId,
                userId,
                blueprint.getCategoryId(),
                blueprint.getTestSeriesId(),
                blueprint.getTestId(),
                timeTakenSeconds,
                buildSectionAnswers(blueprint, answerMap),
                buildTopicAnswers(blueprint, answerMap),
                timestamp
        );
    }

    private static List<AttemptSubmittedEvent.TopicAnswerPayload> buildTopicAnswers(
            InternalTestBlueprintDto blueprint,
            Map<String, String> answers
    ) {
        if (blueprint.getSections() == null) {
            return List.of();
        }

        Map<String, List<InternalTestBlueprintDto.InternalQuestionDto>> questionsByTopic = new LinkedHashMap<>();

        for (InternalTestBlueprintDto.InternalSectionDto section : blueprint.getSections()) {
            if (section.getQuestions() == null) {
                continue;
            }
            for (InternalTestBlueprintDto.InternalQuestionDto question : section.getQuestions()) {
                String topic = (question.getTopic() != null && !question.getTopic().trim().isEmpty())
                        ? question.getTopic().trim()
                        : "General";
                questionsByTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(question);
            }
        }

        List<AttemptSubmittedEvent.TopicAnswerPayload> topicPayloads = new ArrayList<>();
        for (Map.Entry<String, List<InternalTestBlueprintDto.InternalQuestionDto>> entry : questionsByTopic.entrySet()) {
            String topic = entry.getKey();
            List<InternalTestBlueprintDto.InternalQuestionDto> questions = entry.getValue();

            int correct = 0;
            int incorrect = 0;
            int unattempted = 0;

            for (InternalTestBlueprintDto.InternalQuestionDto question : questions) {
                String selected = answers.get(question.getQuestionId());
                if (selected == null || selected.isBlank()) {
                    unattempted++;
                    continue;
                }

                if (isCorrect(question, selected)) {
                    correct++;
                } else {
                    incorrect++;
                }
            }

            int totalQuestions = questions.size();
            double accuracy = totalQuestions == 0
                    ? 0
                    : Math.round((correct * 10000.0) / totalQuestions) / 100.0;

            topicPayloads.add(new AttemptSubmittedEvent.TopicAnswerPayload(
                    topic,
                    totalQuestions,
                    correct,
                    incorrect,
                    unattempted,
                    accuracy,
                    null
            ));
        }

        return topicPayloads;
    }

    private static List<AttemptSubmittedEvent.SectionAnswerPayload> buildSectionAnswers(
            InternalTestBlueprintDto blueprint,
            Map<String, String> answers
    ) {
        if (blueprint.getSections() == null) {
            return List.of();
        }

        return blueprint.getSections().stream()
                .map(section -> {
                    double score = 0;
                    double maxScore = 0;
                    int correct = 0;
                    int incorrect = 0;
                    int unattempted = 0;

                    List<InternalTestBlueprintDto.InternalQuestionDto> questions =
                            section.getQuestions() == null ? List.of() : section.getQuestions();

                    for (InternalTestBlueprintDto.InternalQuestionDto question : questions) {
                        double positiveMarks = decimalValue(question.getPositiveMarks());
                        double negativeMarks = decimalValue(question.getNegativeMarks());

                        maxScore += positiveMarks;

                        String selected = answers.get(question.getQuestionId());

                        if (selected == null || selected.isBlank()) {
                            unattempted++;
                            continue;
                        }

                        if (isCorrect(question, selected)) {
                            correct++;
                            score += positiveMarks;
                        } else {
                            incorrect++;
                            if (Boolean.TRUE.equals(blueprint.getNegativeMarkingEnabled())) {
                                score -= negativeMarks;
                            }
                        }
                    }

                    int totalQuestions = questions.size();
                    double accuracy = totalQuestions == 0
                            ? 0
                            : Math.round((correct * 10000.0) / totalQuestions) / 100.0;

                    return new AttemptSubmittedEvent.SectionAnswerPayload(
                            section.getSectionId(),
                            section.getTitle(),
                            round(score),
                            round(maxScore),
                            totalQuestions,
                            correct,
                            incorrect,
                            unattempted,
                            accuracy,
                            null
                    );
                })
                .toList();
    }

    private static boolean isCorrect(
            InternalTestBlueprintDto.InternalQuestionDto question,
            String selected
    ) {
        if (question.getCorrectAnswer() == null) {
            return false;
        }

        String type = question.getQuestionType();

        if ("MCQ".equalsIgnoreCase(type)) {
            Object correct = question.getCorrectAnswer().get("key");
            return correct != null && correct.toString().trim().equalsIgnoreCase(selected.trim());
        }

        if ("MULTI_CORRECT".equalsIgnoreCase(type)) {
            Object correct = question.getCorrectAnswer().get("keys");
            return normalizeSet(correct).equals(normalizeSet(selected));
        }

        if ("NUMERICAL".equalsIgnoreCase(type)) {
            Object correct = question.getCorrectAnswer().get("value");
            if (correct == null) {
                return false;
            }

            try {
                return new BigDecimal(correct.toString().trim())
                        .compareTo(new BigDecimal(selected.trim())) == 0;
            } catch (NumberFormatException e) {
                return correct.toString().trim().equalsIgnoreCase(selected.trim());
            }
        }

        return false;
    }

    private static Set<String> normalizeSet(Object value) {
        if (value == null) {
            return Set.of();
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(String::toUpperCase)
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        String text = value.toString()
                .trim()
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");

        if (text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static double decimalValue(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}