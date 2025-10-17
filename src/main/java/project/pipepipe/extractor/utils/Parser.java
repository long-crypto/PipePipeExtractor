package project.pipepipe.extractor.utils;

import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static project.pipepipe.extractor.utils.UtilsOld.UTF_8;

/**
 * Avoid using regex !!!
 */
public final class Parser {

    private Parser() {
    }


    public static String matchGroup1(final String pattern, final String input){
        return matchGroup(pattern, input, 1);
    }

    public static String matchGroup1(final Pattern pattern,
                                     final String input) {
        return matchGroup(pattern, input, 1);
    }

    public static String matchGroup(final String pattern,
                                    final String input,
                                    final int group){
        return matchGroup(Pattern.compile(pattern), input, group);
    }

    public static String matchGroup(@Nonnull final Pattern pat,
                                    final String input,
                                    final int group){
        final Matcher matcher = pat.matcher(input);
        final boolean foundMatch = matcher.find();
        if (foundMatch) {
            return matcher.group(group);
        } else {
            // only pass input to exception message when it is not too long
            if (input.length() > 1024) {
                throw new IllegalStateException("Failed to find pattern \"" + pat.pattern() + "\"");
            } else {
                throw new IllegalStateException("Failed to find pattern \"" + pat.pattern()
                        + "\" inside of \"" + input + "\"");
            }
        }
    }

    public static String matchGroup1MultiplePatterns(final Pattern[] patterns, final String input){
        return matchMultiplePatterns(patterns, input).group(1);
    }

    public static String matchGroup2MultiplePatterns(final Pattern[] patterns, final String input) {
        return matchMultiplePatterns(patterns, input).group(2);
    }

    public static Matcher matchMultiplePatterns(final Pattern[] patterns, final String input) {
        IllegalStateException exception = null;
        for (final Pattern pattern : patterns) {
            final Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return matcher;
            } else if (exception == null) {
                // only pass input to exception message when it is not too long
                if (input.length() > 1024) {
                    exception = new IllegalStateException("Failed to find pattern \"" + pattern.pattern()
                            + "\"");
                } else {
                    exception = new IllegalStateException("Failed to find pattern \"" + pattern.pattern()
                            + "\" inside of \"" + input + "\"");
                }
            }
        }

        if (exception == null) {
            throw new IllegalStateException("Empty patterns array passed to matchMultiplePatterns");
        } else {
            throw exception;
        }
    }

    public static boolean isMatch(final String pattern, final String input) {
        final Pattern pat = Pattern.compile(pattern);
        final Matcher mat = pat.matcher(input);
        return mat.find();
    }

    public static boolean isMatch(@Nonnull final Pattern pattern, final String input) {
        final Matcher mat = pattern.matcher(input);
        return mat.find();
    }

    @Nonnull
    public static Map<String, String> compatParseMap(@Nonnull final String input)
            throws UnsupportedEncodingException {
        final Map<String, String> map = new HashMap<>();
        for (final String arg : input.split("&")) {
            final String[] splitArg = arg.split("=");
            if (splitArg.length > 1) {
                map.put(splitArg[0], URLDecoder.decode(splitArg[1], UTF_8));
            } else {
                map.put(splitArg[0], "");
            }
        }
        return map;
    }

    @Nonnull
    public static String[] getLinksFromString(final String txt) {
        try {
            final List<String> links = new ArrayList<>();
            final LinkExtractor linkExtractor = LinkExtractor.builder()
                    .linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW))
                    .build();
            final Iterable<LinkSpan> linkSpans = linkExtractor.extractLinks(txt);
            for (final LinkSpan ls : linkSpans) {
                links.add(txt.substring(ls.getBeginIndex(), ls.getEndIndex()));
            }

            String[] linksarray = new String[links.size()];
            linksarray = links.toArray(linksarray);
            return linksarray;
        } catch (final Exception e) {
            throw new IllegalStateException("Could not get links from string", e);
        }
    }
}
