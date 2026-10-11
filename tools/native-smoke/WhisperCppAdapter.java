package com.mudmap.adapters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

// Host-only harness for the production instance JNI symbol and field contract.
public class WhisperCppAdapter {
    public final String modelPath;

    public WhisperCppAdapter(String modelPath) {
        this.modelPath = modelPath;
    }

    public native String whisperTranscribe(float[] samples);

    public static void main(String[] args) throws Exception {
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        ByteBuffer bytes = ByteBuffer.wrap(Files.readAllBytes(Path.of(args[2])))
            .order(ByteOrder.LITTLE_ENDIAN);
        float[] samples = new float[bytes.remaining() / Float.BYTES];
        bytes.asFloatBuffer().get(samples);
        String transcript = new WhisperCppAdapter(args[1]).whisperTranscribe(samples);
        System.out.println("Transcript: " + transcript.trim());
        if (!transcript.toLowerCase(Locale.ROOT).contains("fellow americans")) {
            throw new AssertionError("Unexpected native transcript.");
        }
        System.out.println("Production JNI smoke test passed.");
    }
}
