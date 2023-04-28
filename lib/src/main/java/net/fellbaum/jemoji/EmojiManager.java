package net.fellbaum.jemoji;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class EmojiManager {

    private static final String PATH = "emojis.json";

    private static final Map<String, Emoji> EMOJI_CHAR_TO_EMOJI;
    private static final Map<Character, List<Emoji>> EMOJI_FIRST_CHAR_TO_EMOJIS_ORDER_CHAR_LENGTH_DESCENDING;
    private static final List<Emoji> EMOJIS_LENGTH_DESCENDING;

    private static final Pattern EMOJI_PATTERN;
    private static final Pattern NOT_WANTED_EMOJI_CHARACTERS = Pattern.compile("[\\p{Alpha}\\p{Z}]");

    //WEG MACHEN?
    private static final Pattern EMOJI_PATTERN_REMOVE_OTHER_CHARS;


    private static final Comparator<Emoji> EMOJI_CHAR_COMPARATOR = (Emoji o1, Emoji o2) -> {
        if (o1.getEmoji().length() == o2.getEmoji().length()) return 0;
        return o1.getEmoji().length() > o2.getEmoji().length() ? -1 : 1;
    };

    static {
        final String fileContent = readFileAsString();
        try {
            final List<Emoji> emojis = new ObjectMapper().readValue(fileContent, new TypeReference<List<Emoji>>() {
                    }).stream()
                    .filter(emoji -> emoji.getQualification() == Qualification.FULLY_QUALIFIED || emoji.getQualification() == Qualification.COMPONENT)
                    .collect(Collectors.toList());

            EMOJI_CHAR_TO_EMOJI = Collections.unmodifiableMap(
                    emojis.stream().collect(Collectors.toMap(Emoji::getEmoji, Function.identity()))
            );

            EMOJIS_LENGTH_DESCENDING = Collections.unmodifiableList(emojis.stream().sorted(EMOJI_CHAR_COMPARATOR).collect(Collectors.toList()));

            EMOJI_FIRST_CHAR_TO_EMOJIS_ORDER_CHAR_LENGTH_DESCENDING = emojis.stream().collect(Collectors.groupingBy(
                    emoji -> emoji.getEmoji().charAt(0),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                list.sort(EMOJI_CHAR_COMPARATOR);
                                return list;
                            }
                    )
            ));

            EMOJI_PATTERN_REMOVE_OTHER_CHARS = Pattern.compile("[^" + EMOJI_CHAR_TO_EMOJI.keySet().stream().map(Pattern::quote).collect(Collectors.joining("|")) + "]");
            EMOJI_PATTERN = Pattern.compile(EMOJIS_LENGTH_DESCENDING.stream()
                    .map(s -> "(" + Pattern.quote(s.getEmoji()) + ")").collect(Collectors.joining("|")), Pattern.UNICODE_CHARACTER_CLASS);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFileAsString() {
        try {
            final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try (final InputStream is = classLoader.getResourceAsStream(PATH)) {
                if (is == null) return null;
                try (final InputStreamReader isr = new InputStreamReader(is);
                     final BufferedReader reader = new BufferedReader(isr)) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the emoji for the given unicode.
     *
     * @param emoji The unicode of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getEmoji(final String emoji) {
        if (isStringNullOrEmpty(emoji)) return Optional.empty();
        return Optional.ofNullable(EMOJI_CHAR_TO_EMOJI.get(emoji));
    }

    /**
     * Check if the given string is an emoji.
     *
     * @param emoji The emoji to check.
     * @return True if the given string is an emoji.
     */
    public static boolean isEmoji(final String emoji) {
        if (isStringNullOrEmpty(emoji)) return false;
        return EMOJI_CHAR_TO_EMOJI.containsKey(emoji);
    }

    /**
     * Gets all emojis.
     *
     * @return A set of all emojis.
     */
    public static Set<Emoji> getAllEmojis() {
        return new HashSet<>(EMOJIS_LENGTH_DESCENDING);
    }

    /**
     * Gets all emojis in descending order by their char length.
     *
     * @return A list of all emojis.
     */
    public static List<Emoji> getAllEmojisLengthDescending() {
        return EMOJIS_LENGTH_DESCENDING;
    }

    /**
     * Gets an emoji for the given alias i.e. :thumbsup: if present.
     *
     * @param alias The alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String checkedAlias = removeColonFromAlias(alias);
        return EMOJI_CHAR_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getAllAliases().contains(checkedAlias))
                .findFirst();
    }

    /**
     * Gets an emoji for the given Discord alias i.e. :thumbsup: if present.
     *
     * @param alias The Discord alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByDiscordAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String checkedAlias = removeColonFromAlias(alias);
        return EMOJI_CHAR_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getDiscordAliases().contains(checkedAlias))
                .findFirst();
    }

    /**
     * Gets an emoji for the given GitHub alias i.e. :thumbsup: if present.
     *
     * @param alias The GitHub alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getByGithubAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String checkedAlias = removeColonFromAlias(alias);
        return EMOJI_CHAR_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getGithubAliases().contains(checkedAlias))
                .findFirst();
    }

    /**
     * Gets an emoji for the given Slack alias i.e. :thumbsup: if present.
     *
     * @param alias The Slack alias of the emoji.
     * @return The emoji.
     */
    public static Optional<Emoji> getBySlackAlias(final String alias) {
        if (isStringNullOrEmpty(alias)) return Optional.empty();
        final String checkedAlias = removeColonFromAlias(alias);
        return EMOJI_CHAR_TO_EMOJI.values()
                .stream()
                .filter(emoji -> emoji.getSlackAliases().contains(checkedAlias))
                .findFirst();
    }

    private static String removeColonFromAlias(final String alias) {
        return alias.startsWith(":") && alias.endsWith(":") ? alias.substring(1, alias.length() - 1) : alias;
    }

    /**
     * Gets the pattern checking for all emojis.
     *
     * @return The pattern for all emojis.
     */
    public static Pattern getEmojiPattern() {
        return EMOJI_PATTERN;
    }

    /**
     * Checks if the given text contains emojis.
     *
     * @param text The text to check.
     * @return True if the given text contains emojis.
     */
    public static boolean containsEmoji(final String text) {
        if (isStringNullOrEmpty(text)) return false;
        return EMOJI_CHAR_TO_EMOJI.keySet().stream().anyMatch(text::contains);
    }

    /**
     * Extracts all emojis from the given text in the order they appear.
     *
     * @param text The text to extract emojis from.
     * @return A list of emojis.
     */
    public static List<Emoji> extractEmojisInOrder(String text) {
        text = truncateString(text);
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final List<Emoji> emojis = new ArrayList<>();

        final int textLength = text.length();

        for (int textIndex = 0; textIndex < textLength; textIndex++) {
            final List<Emoji> emojisByChar = EMOJI_FIRST_CHAR_TO_EMOJIS_ORDER_CHAR_LENGTH_DESCENDING.get(text.charAt(textIndex));
            if (emojisByChar == null) continue;
            for (Emoji emoji : emojisByChar) {
                final int emojiCharLength = emoji.getEmoji().length();
                if ((textIndex + emojiCharLength) <= textLength) {
                    for (int i = 0; i < emojiCharLength; i++) {
                        if (text.charAt(textIndex + i) != emoji.getEmoji().charAt(i)) {
                            break;
                        }
                        if (i == emojiCharLength - 1) {
                            emojis.add(emoji);
                            textIndex += emojiCharLength - 1;
                            break;
                        }
                    }
                    /*
                    if (text.substring(textIndex, textIndex + emojiCharLength).equals(emoji.getEmoji())) {
                        emojis.add(emoji);
                        textIndex += emojiCharLength - 1;
                        break;
                    }
                     */
                }
            }
        }
        return emojis;
    }

    /**
     * Extracts all emojis from the given text.
     *
     * @param text The text to extract emojis from.
     * @return A list of emojis.
     */
    public static Set<Emoji> extractEmojis(final String text) {
        return new HashSet<>(extractEmojisInOrder(text));
    }

    /**
     * Removes all emojis from the given text.
     *
     * @param text The text to remove emojis from.
     * @return The text without emojis.
     */
    public static String removeAllEmojis(final String text) {
        return EMOJI_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Removes the given emojis from the given text.
     *
     * @param text           The text to remove emojis from.
     * @param emojisToRemove The emojis to remove.
     * @return The text without the given emojis.
     */
    public static String removeEmojis(String text, final Collection<Emoji> emojisToRemove) {
        if (isStringNullOrEmpty(text)) return "";

        for (final Emoji emoji : emojisToRemove) {
            text = text.replaceAll(Pattern.quote(emoji.getEmoji()), "");
        }

        return text;
    }

    /**
     * Removes all emojis except the given emojis from the given text.
     *
     * @param text         The text to remove emojis from.
     * @param emojisToKeep The emojis to keep.
     * @return The text with only the given emojis.
     */
    public static String removeAllEmojisExcept(final String text, final Collection<Emoji> emojisToKeep) {
        final Set<Emoji> emojisToRemove = new HashSet<>(EMOJIS_LENGTH_DESCENDING);
        emojisToRemove.removeAll(emojisToKeep);

        return removeEmojis(text, emojisToRemove);
    }

    /**
     * Removes all emojis except the given emojis from the given text.
     *
     * @param text         The text to remove emojis from.
     * @param emojisToKeep The emojis to keep.
     * @return The text with only the given emojis.
     */
    public static String removeAllEmojisExcept(String text, final Emoji... emojisToKeep) {
        return removeAllEmojisExcept(text, Arrays.asList(emojisToKeep));
    }

    /**
     * Replaces all emojis in the text with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param replacementString The replacement string.
     * @return The text with all emojis replaced.
     */
    public static String replaceAllEmojis(String text, final String replacementString) {
        return replaceEmojis(text, replacementString, EMOJIS_LENGTH_DESCENDING);
    }

    /**
     * Replaces the given emojis with the given replacement string.
     *
     * @param text              The text to replace emojis from.
     * @param emojisToReplace   The emojis to replace.
     * @param replacementString The replacement string.
     * @return The text with the given emojis replaced.
     */
    public static String replaceEmojis(String text, final String replacementString, final Collection<Emoji> emojisToReplace) {
        if (isStringNullOrEmpty(text)) return "";

        for (final Emoji emoji : emojisToReplace) {
            text = text.replace(emoji.getEmoji(), replacementString);
        }

        return text;
    }

    private static String truncateString(final String text) {
        if (isStringNullOrEmpty(text)) return "";

        final Matcher matcher = NOT_WANTED_EMOJI_CHARACTERS.matcher(text);
        return matcher.replaceAll("");
    }

    private static boolean isStringNullOrEmpty(final String string) {
        return null == string || string.isEmpty();
    }


    /*public static List<Emoji> testEmojiPattern(final String text) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final Matcher matcher = EMOJI_PATTERN.matcher(text);

        final List<Emoji> emojis = new ArrayList<>();
        while (matcher.find()) {
            emojis.add(EMOJIS_LENGTH_DESCENDING.stream().filter(emoji -> emoji.getEmoji().equals(matcher.group())).findFirst().get());
        }
        return Collections.unmodifiableList(emojis);
    }*/

        /*public static List<Emoji> extractEmojisInOrderEmojiRegex(String text) {
        if (isStringNullOrEmpty(text)) return Collections.emptyList();

        final List<Emoji> emojis = new ArrayList<>();
        System.out.println(EMOJI_PATTERN.pattern());
        System.out.println(EMOJI_PATTERN.toString());

        Matcher matcher = EMOJI_PATTERN.matcher(text);
        while (matcher.find()) {
            String emoji = matcher.group();

            emojis.add(EMOJI_CHAR_TO_EMOJI.get(emoji));
        }

        return emojis;
    }*/
}

