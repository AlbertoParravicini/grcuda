#pragma once
#include "benchmark.cuh"

class Benchmark11 : public Benchmark
{
public:
    Benchmark11(Options &options) : Benchmark(options) {}
    void alloc();
    void init();
    void reset();
    void execute_sync(int iter);
    void execute_async(int iter);
    void execute_cudagraph(int iter);
    void execute_cudagraph_manual(int iter);
    void first();
    void second();
    std::string print_result(bool short_form = false);

private:
    float *x, *y, *x1, *y1, *res;
    cudaStream_t s1, s2;
};