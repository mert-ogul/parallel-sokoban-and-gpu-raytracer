package nl.tue.pdc.sampling;

public class GPURandom {

    private long seed;

    public GPURandom(long seed) {
        this.seed = seed;
    }

    public float random(float low, float high) {
        // Treat the 64-bit seed as two 32-bit integers (v0, v1)
        int v0 = (int) (seed & 0xFFFFFFFFL);
        int v1 = (int) (seed >>> 32);
        
        // TEA Constants (the "Key")
        int k0 = 0xA54FF53A; 
        int k1 = 0x3C6EF9AC;
        int k2 = 0x8D2E1234;
        int k3 = 0x76543210;
        
        int sum = 0;
        int delta = 0x9E3779B9;

        // 32 rounds of TEA - very compact IR node count for TornadoVM
        for (int i = 0; i < 32; i++) {
            sum += delta;
            v0 += ((v1 << 4) + k0) ^ (v1 + sum) ^ ((v1 >> 5) + k1);
            v1 += ((v0 << 4) + k2) ^ (v0 + sum) ^ ((v0 >> 5) + k3);
        }

        // Increment the original seed for the next call to ensure a new sequence
        seed += 0x9E3779B97F4A7C15L;

        // Use the scrambled v0 for the random float (24 bits of entropy)
        float normalized = (v0 & 0xFFFFFF) / (float) (1 << 24);
        
        return low + normalized * (high - low);
    }
}