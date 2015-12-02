package com.example.android.guitartuner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FrequencyDetector {

    public static float[] absAndHalve(float[] in) {
        // retain magnitude of in and discard half of points
        float[] result = new float[in.length / 2];
        for (int i = 0; i < in.length/2; i++) {
            result[i] = Math.abs(in[i]);
        }

        return result;
    }

    public static float[] convertToFloats(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    public static int[] identifyPeaksAndRemove(float[] in) {
        final int N_PEAKS = 5; // number of peaks to save
        final int PEAK_THRESHOLD = 15; // number of frequencies to remove
                                      // from each side of a peak

        int len = in.length;
        int[] result = new int[N_PEAKS];

        for(int peak = 0; peak < N_PEAKS; peak++) {
            float max = 0;
            for(int i = 0; i < len; i++) {
                if(in[i] > max) {
                    max = in[i];
                    result[peak] = i;
                }
            }

            // remove detected peak, and surrounding frequencies up to PEAK_THRESHOLD
            for(int i = 0; i < PEAK_THRESHOLD; i++) {
                if(result[peak] > PEAK_THRESHOLD) in[result[peak] - i] = 0; // if statements prevent outofbounds errors
                if(result[peak] < len - PEAK_THRESHOLD) in[result[peak] + i] = 0;
            }

        }
        return result;
    }

    public static int detectFrequency(int[] input) {
        final int DOUBLE_TOLERANCE = 6;

        // Check for smallest value in input array (fundamental frequency),
        // and to confirm that it is correct, check that there is another
        // element in the peaks array that is about double its value

        int fundamentalFreq = 12345;
        for(int i = 0; i < input.length; i++) {
            if(input[i] < fundamentalFreq && hasDouble(input[i], input) && input[i] > 40 && input[i] < 360)
                fundamentalFreq = input[i];
        }

        if(fundamentalFreq == 12345) fundamentalFreq = -1;

        return fundamentalFreq;
    }

    private static boolean hasDouble(int n, int[] arr) {
        final int DOUBLE_TOLERANCE = 6;

        for(int i = 0; i < arr.length; i++) {
            if(Math.abs(n*2 - arr[i]) <= DOUBLE_TOLERANCE) return true;
        }

        return false;
    }

    public static int detectStringFreq(int freq) {
        int[] stringFrequencies = {82, 110, 147, 196, 247, 330};

        int result = -1;
        int min = 12345;

        for(int i = 0; i < stringFrequencies.length; i++) {
            int dFromCurrentToDesired = Math.abs(stringFrequencies[i] - freq);
            if(dFromCurrentToDesired < min) {
                min = dFromCurrentToDesired;
                result = stringFrequencies[i];
            }
        }

        return result;
    }

    public static String getStringName(int freq) {
        int[] stringFrequencies = {82, 110, 147, 196, 247, 330};
        switch (freq) {
            case 82: return "E";
            case 110: return "A";
            case 147: return "D";
            case 196: return "G";
            case 247: return "B";
            case 330: return "E";
            default: return "?";
        }
    }

    public static void saveDataToFile(String name, float[] x) {
        try {
            String filename = "/sdcard/" + name + ".txt";
            BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename));

            for (double e : x) {
                outputWriter.write(e+"");
                outputWriter.newLine();
            }

            outputWriter.flush();
            outputWriter.close();

        } catch(IOException e) {

            e.printStackTrace();

        }
    }

    public static void saveDataToFile(String name, short[] x) {
        try {
            String filename = "/sdcard/" + name + ".txt";
            BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename));

            for (short e : x) {
                outputWriter.write(e+"");
                outputWriter.newLine();
            }

            outputWriter.flush();
            outputWriter.close();

        } catch(IOException e) {

            e.printStackTrace();

        }
    }

}