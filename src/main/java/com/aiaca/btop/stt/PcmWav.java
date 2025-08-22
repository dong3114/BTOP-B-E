//JJ
package com.aiaca.btop.stt;

import java.io.*;

public class PcmWav {
  public static byte[] wrapMono16leToWav(byte[] pcm, int sampleRate) throws IOException {
    int channels = 1;
    int bitsPerSample = 16;
    int byteRate = sampleRate * channels * bitsPerSample / 8;
    int blockAlign = channels * bitsPerSample / 8;
    int dataLen = pcm.length;
    int chunkSize = 36 + dataLen;

    ByteArrayOutputStream baos = new ByteArrayOutputStream(44 + dataLen);
    DataOutputStream out = new DataOutputStream(baos);
    out.writeBytes("RIFF");
    out.writeInt(Integer.reverseBytes(chunkSize));
    out.writeBytes("WAVE");
    out.writeBytes("fmt ");
    out.writeInt(Integer.reverseBytes(16));             // PCM subchunk size
    out.writeShort(Short.reverseBytes((short)1));       // PCM format
    out.writeShort(Short.reverseBytes((short)channels));
    out.writeInt(Integer.reverseBytes(sampleRate));
    out.writeInt(Integer.reverseBytes(byteRate));
    out.writeShort(Short.reverseBytes((short)blockAlign));
    out.writeShort(Short.reverseBytes((short)bitsPerSample));
    out.writeBytes("data");
    out.writeInt(Integer.reverseBytes(dataLen));
    out.write(pcm);
    out.flush();
    return baos.toByteArray();
  }
}
