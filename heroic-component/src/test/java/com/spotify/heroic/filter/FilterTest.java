package com.spotify.heroic.filter;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static com.spotify.heroic.filter.Filter.and;
import static com.spotify.heroic.filter.Filter.hasTag;
import static com.spotify.heroic.filter.Filter.matchTag;
import static com.spotify.heroic.filter.Filter.not;
import static com.spotify.heroic.filter.Filter.or;
import static com.spotify.heroic.filter.Filter.startsWith;
import static org.junit.Assert.assertEquals;

public class FilterTest {
    private final Filter a = hasTag("a");
    private final Filter b = hasTag("b");
    private final Filter c = hasTag("c");
    private final Filter tag1a = matchTag("foo", "a");
    private final Filter tag1b = matchTag("foo", "b");

    private final Filter s1a = startsWith("foo", "abc");
    private final Filter s1b = startsWith("foo", "abcdef");

    /**
     * {@link com.spotify.heroic.filter.Filter.Visitor} methods should all defer to {@link
     * com.spotify.heroic.filter.Filter.Visitor#defaultAction(Filter)} unless defined.
     */
    @Test
    public void visitorDefaultActionTest() {
        final Object ref = new Object();
        final Filter.Visitor<Object> v = filter -> ref;

        assertEquals(ref, v.visitStartsWith(Mockito.mock(StartsWithFilter.class)));
        assertEquals(ref, v.visitHasTag(Mockito.mock(HasTagFilter.class)));
        assertEquals(ref, v.visitNot(Mockito.mock(NotFilter.class)));
        assertEquals(ref, v.visitTrue(Mockito.mock(TrueFilter.class)));
        assertEquals(ref, v.visitFalse(Mockito.mock(FalseFilter.class)));
        assertEquals(ref, v.visitMatchTag(Mockito.mock(MatchTagFilter.class)));
        assertEquals(ref, v.visitMatchKey(Mockito.mock(MatchKeyFilter.class)));
        assertEquals(ref, v.visitAnd(Mockito.mock(AndFilter.class)));
        assertEquals(ref, v.visitOr(Mockito.mock(OrFilter.class)));
        assertEquals(ref, v.visitRaw(Mockito.mock(RawFilter.class)));
        assertEquals(ref, v.visitRegex(Mockito.mock(RegexFilter.class)));
    }

    @Test
    public void optimizeAndTest() {
        final Filter ref = and(a, b).optimize();
        Assert.assertTrue(ref instanceof AndFilter);
        assertEquals(2, ((AndFilter) ref).terms().size());
        assertEquals(ref, and(a, b, b).optimize());
        assertEquals(ref, and(b, a).optimize());
        assertEquals(ref, and(b, and(a, b)).optimize());
    }

    @Test
    public void optimizeOrTest() {
        final Filter ref = or(a, b).optimize();
        Assert.assertTrue(ref instanceof OrFilter);
        assertEquals(2, ((OrFilter) ref).terms().size());
        assertEquals(ref, or(a, b, b).optimize());
        assertEquals(ref, or(b, a).optimize());
        assertEquals(ref, or(b, or(a, b)).optimize());
    }

    @Test
    public void testSortOrder() {
        final List<List<Filter>> inputs =
            ImmutableList.<List<Filter>>of(ImmutableList.of(a, b, c), ImmutableList.of(c, b, a),
                ImmutableList.of(c, b, a));

        final List<Filter> reference = ImmutableList.of(a, b, c);

        for (final List<Filter> input : inputs) {
            final List<Filter> sorted = new ArrayList<>(new TreeSet<>(input));
            assertEquals(reference, sorted);
        }
    }

    @Test
    public void testOrFlatten() {
        assertEquals(or(a, b, c), or(a, or(b, c)).optimize());
        assertEquals(or(a, b, c), or(a, or(b, or(c))).optimize());
        assertEquals(or(a, b, c), or(a, not(and(not(b), not(c)))).optimize());
    }

    @Test
    public void testAndFlatten() {
        assertEquals(and(a, b, c), and(a, and(b, c)).optimize());
        assertEquals(and(a, b, c), and(a, and(b, and(c))).optimize());
        assertEquals(and(a, b, c), and(a, not(or(not(b), not(c)))).optimize());
    }

    @Test
    public void optimizeNotNot() {
        assertEquals(a, not(not(a)).optimize());
    }

    /**
     * The same expression, but inverted cannot co-exist in the same and statement.
     */
    @Test
    public void testAndContradiction() throws Exception {
        // same filter inverted
        assertEquals(FalseFilter.get(), and(a, not(a)).optimize());
        // same tag with different values
        assertEquals(FalseFilter.get(), and(tag1a, tag1b).optimize());
    }

    /**
     * The same expression, but inverted cannot co-exist in the same and statement.
     */
    @Test
    public void testOrTautology() throws Exception {
        assertEquals(TrueFilter.get(), or(a, not(a)).optimize());
    }

    @Test
    public void testAndStartsWith() {
        assertEquals(s1b, and(s1a, s1b).optimize());
        assertEquals(s1b, and(s1b, s1a).optimize());
    }

    @Test
    public void testOrStartsWith() {
        assertEquals(s1a, or(s1a, s1b).optimize());
        assertEquals(s1a, or(s1b, s1a).optimize());
    }
}
