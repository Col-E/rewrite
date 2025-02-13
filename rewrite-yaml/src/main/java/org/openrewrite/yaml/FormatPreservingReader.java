/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml;

import org.openrewrite.internal.lang.NonNull;
import org.yaml.snakeyaml.events.Event;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Maintains a sliding buffer of characters used to determine format prefixes of
 * YAML AST elements.
 */
class FormatPreservingReader extends Reader {

    private final Reader delegate;

    private ArrayList<Character> buffer = new ArrayList<>();
    private int bufferIndex = 0;

    FormatPreservingReader(Reader delegate) {
        this.delegate = delegate;
    }

    String prefix(int lastEnd, int startIndex) {
        assert lastEnd <= startIndex;

        int prefixLen = startIndex - lastEnd;
        if (prefixLen > 0) {
            char[] prefix = new char[prefixLen];

            for (int i = 0; i < prefixLen; i++) {
                prefix[i] = buffer.get(lastEnd - bufferIndex + i);
            }
            if (lastEnd > bufferIndex) {
                buffer = new ArrayList<>(buffer.subList(lastEnd - bufferIndex, buffer.size()));
                bufferIndex = lastEnd;
            }

            return new String(prefix);
        }
        return "";
    }

    public String prefix(int lastEnd, Event event) {
        return prefix(lastEnd, event.getStartMark().getIndex());
    }

    public String readStringFromBuffer(int start, int end) {
        int length = end - start + 1;
        char[] readBuff = new char[length];
        for (int i = 0; i < length; i++) {
            int bufferOffset = start + i - bufferIndex;
            readBuff[i] = buffer.get(bufferOffset);
        }
        return new String(readBuff);
    }

    @Override
    public int read(@NonNull char[] cbuf, int off, int len) throws IOException {
        int read = delegate.read(cbuf, off, len);
        if (read > 0) {
            buffer.ensureCapacity(buffer.size() + read);
            for (int i = 0; i < read; i++) {
                char e = cbuf[i];
                if (Character.UnicodeBlock.of(e) != Character.UnicodeBlock.BASIC_LATIN && i % 2 == 0) {
                    bufferIndex--;
                }
                buffer.add(e);
            }
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
