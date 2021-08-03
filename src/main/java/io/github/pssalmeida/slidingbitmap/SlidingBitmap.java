/**
 * Copyright 2020 Paulo Sergio Almeida
 * SPDX-License-Identifier: Apache-2.0
 * */

package io.github.pssalmeida.slidingbitmap;

import java.util.Arrays;

/**
 * Resizable bitmap suitable for representing a set of element in a sliding window.
 * Elements are added as ranges of new elements contiguous to the previous
 * range. Elements can be removed or tested for presence, but can only be
 * added within the current window. The representation grows as needed and
 * discards memory corresponding to elements before the smallest element still
 * present (start of sliding window).
 *
 * @author Paulo Sergio Almeida
 *
 * */
public class SlidingBitmap {

    private long[] bits;     // bitmap; with 1s from end-offset bit positions onwards
    private long offset = 0; // offset of bit positions in bits; multiple of 64
    private long start = 0;  // start of sliding window: first element when start < end
    private long end = 0;    // end of sliding window, always mapped in bits to a sentinel 1
    private long size = 0;   // number of elements in the set

    /**
     * Constructs an empty set with a default initial capacity.
     */
    public SlidingBitmap() {
        this(48*8);
    }

    /**
     * Constructs an empty set with given initial capacity.
     *
     * @param capacity initial capacity
     */
    public SlidingBitmap(int capacity) {
        int words = (Math.max(0, capacity) >>> 6) + 1;
        bits = new long[words];
        Arrays.fill(bits, ~0L);
    }

    /**
     * Returns start of sliding window: first element, if it exists, or end position.
     *
     * @return start of sliding window: first element, if it exists, or end position
     */
    public long start() {
        return start;
    }

    /**
     * Returns end of sliding window, the starting position for next extension.
     *
     * @return end of sliding window, the starting position for next extension
     */
    public long end() {
        return end;
    }

    /**
     * Returns number of elements in the set.
     *
     * @return number of elements in the set
     */
    public long size() {
        return size;
    }

    /**
     * Returns whether the set contains no elements.
     *
     * @return whether the set contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns whether element is present in the set.
     *
     * @param element element to be tested for presence
     * @return whether element is present in the set
     */
    public boolean contains(long element) {
        if (element < start || element >= end)
            return false;
        int word = word(element);
        long mask = 1L << element;
        return (bits[word] & mask) != 0;
    }

    /**
     * Adds element to set, if absent, in current sliding window.
     * Returns whether element was absent.
     *
     * @param element element to be added; must be in current window
     * @return whether element was present
     */
    public boolean add(long element) {
        if (element < start || element >= end)
            throw new IllegalArgumentException();
        int word = word(element);
        long mask = 1L << element;
        if ((bits[word] & mask) != 0)
            return false;
        bits[word] |= mask;
        ++size;
        return true;
    }

    /**
     * Removes element from set, if present.
     * Returns whether element was present.
     *
     * @param element element to be removed
     * @return whether element was present
     */
    public boolean remove(long element) {
        if (element < start || element >= end)
            return false;
        int word = word(element);
        long mask = 1L << element;
        if ((bits[word] & mask) == 0)
            return false;
        bits[word] &= ~mask;
        --size;
        if (element == start)
            advanceStart(word);
        return true;
    }

    /**
     * Removes elements smaller than given limit.
     * Returns number of elements removed. Does not advance end of sliding
     * window.
     *
     * @param limit one past greatest element to remove
     * @return number of elements removed 
     */
    public long removeSmallerThan(long limit) {
        if (limit <= start)
            return 0;
        if (limit >= end) {
            start = end;
            long n = size;
            size = 0;
            return n;
        }
        int word = word(start);
        int endWord = word(limit);
        long sum = 0;
        while (word < endWord) {
            sum += Long.bitCount(bits[word]);
            bits[word] = 0;
            ++word;
        }
        long mask = (1L << limit) - 1;
        sum += Long.bitCount(bits[word] & mask);
        bits[word] &= ~mask;
        size -= sum;
        advanceStart(word);
        return sum;
    }

    /**
     * Extends set, and sliding window, by a range of elements starting from
     * the current end. Uses {@code reallocFactor} of 1.
     *
     * @param newEnd one past final element in the range added
     */
    public void extendTo(long newEnd) {
        extendTo(newEnd, 1);
    }

    /**
     * Extends set, and sliding window, by a range of elements starting from
     * the current end.
     *
     * @param newEnd one past final element in the range added
     * @param reallocFactor ratio of extra memory to needed memory when reallocating
     */
    public void extendTo(long newEnd, int reallocFactor) {
        if (newEnd <= end)
            return;
        int word = word(newEnd);
        if (word >= bits.length)
            realloc(word, reallocFactor);
        size += newEnd - end;
        end = newEnd;
    }

    private int word(long pos) {
        return (int)((pos - offset) >>> 6);
    }

    private void advanceStart(int word) {
        while (bits[word] == 0) //  there is always a sentinel 1 bit at end
            ++word;
        start = Long.numberOfTrailingZeros(bits[word]) + ((long)word << 6) + offset;
    }

    private void realloc(int word, int reallocFactor) {
        int from = word(start);
        int to = from + (1 + reallocFactor) * (word - from + 1);
        long[] a = Arrays.copyOfRange(bits, from, to);
        Arrays.fill(a, bits.length - from, a.length, ~0L);
        bits = a;
        offset += (long)from << 6;
    }
}
