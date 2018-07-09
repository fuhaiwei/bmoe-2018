package fuhaiwei.bmoe2018.utils;

import java.util.Objects;
import java.util.function.Consumer;

public class Permutations<T> {

    private final T[] source;
    private final T[] output;
    private final int[] status;

    public Permutations(T[] source, T[] output) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(output);
        if (output.length > source.length) {
            String errMsg = String.format("output length: %d > source length: %d",
                    output.length, source.length);
            throw new IllegalArgumentException(errMsg);
        }
        if (output.length == 0) {
            throw new IllegalArgumentException("output length is zero");
        }
        this.source = source;
        this.output = output;
        this.status = new int[output.length];
    }

    public void forEach(Consumer<T[]> consumer) {
        int outLen = output.length;
        int srcLen = source.length;
        set(0, 0, outLen);

        LOOP:
        while (true) {
            consumer.accept(output);
            for (int len = 1; len <= outLen; len++) {
                int idx = outLen - len;
                if (status[idx] + len < srcLen) {
                    set(idx, status[idx] + 1, len);
                    continue LOOP;
                }
            }
            break;
        }
    }

    private void set(int outputIndex, int sourceIndex, int length) {
        for (int i = 0; i < length; i++) {
            output[outputIndex + i] = source[sourceIndex + i];
            status[outputIndex + i] = sourceIndex + i;
        }
    }

}
